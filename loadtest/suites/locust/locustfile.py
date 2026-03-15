import os
import random
import time
from locust import HttpUser, task, between

BASE_URL = os.getenv("BASE_URL", "https://stg.tasteam.kr")
TEST_AUTH_TOKEN_PATH = os.getenv("TEST_AUTH_TOKEN_PATH", "/api/v1/test/auth/token")
USER_ID_MAX = int(os.getenv("USER_ID_MAX", "1000"))
TEST_GROUP_CODE = os.getenv("TEST_GROUP_CODE", "LOCAL-1234")
GROUP_SEARCH_KEYWORDS = [
    keyword.strip()
    for keyword in os.getenv("GROUP_SEARCH_KEYWORDS", "테스트").split(",")
    if keyword.strip()
]

SEARCH_KEYWORDS = ["파스타", "치킨", "피자", "카페", "강남", "성수", "점심", "회식", "가성비"]


class TasteamUser(HttpUser):
    host = BASE_URL
    wait_time = between(1, 5)

    def on_start(self):
        self.token = None
        self.group_id = None
        self.subgroup_id = None
        self.chat_room_id = None
        self.restaurant_id = None

        uid = random.randint(1, USER_ID_MAX)
        login_body = {
            "identifier": f"test-user-{uid:03d}",
            "nickname": f"부하테스트계정{uid}",
        }

        with self.client.post(TEST_AUTH_TOKEN_PATH, json=login_body, name="test/auth/token", catch_response=True) as res:
            if res.status_code == 200:
                try:
                    self.token = res.json().get("data", {}).get("accessToken")
                    if self.token:
                        res.success()
                    else:
                        res.failure("accessToken missing")
                except Exception as exc:
                    res.failure(f"token parse failed: {exc}")
            else:
                res.failure(f"login failed: {res.status_code}")

        if not self.token:
            return

        headers = self._auth_headers()
        self._ensure_group_context(headers)

    def _auth_headers(self):
        return {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json",
        }

    def _extract_restaurant_ids_from_sections(self, payload):
        ids = []
        sections = payload.get("data", {}).get("sections", [])
        for section in sections:
            for item in section.get("items", []):
                restaurant_id = item.get("restaurantId") or item.get("id")
                if restaurant_id and restaurant_id not in ids:
                    ids.append(restaurant_id)
        return ids

    def _extract_restaurant_ids_from_search(self, payload):
        ids = []
        restaurants = payload.get("data", {}).get("restaurants", {}).get("items", [])
        for item in restaurants:
            restaurant_id = item.get("restaurantId") or item.get("id")
            if restaurant_id and restaurant_id not in ids:
                ids.append(restaurant_id)
        return ids

    def _ensure_group_context(self, headers):
        r = self.client.get("/api/v1/members/me/groups", headers=headers, name="members/me/groups")
        if r.status_code == 200:
            try:
                data = r.json().get("data", {})
                items = data.get("items", []) if isinstance(data, dict) else data
                if items:
                    self.group_id = items[0].get("id")
            except Exception:
                pass

        if not self.group_id:
            candidate_group_ids = self._search_group_candidates(headers)
            for group_id in candidate_group_ids:
                join_body = {"code": TEST_GROUP_CODE}
                j = self.client.post(
                    f"/api/v1/groups/{group_id}/password-authentications",
                    json=join_body,
                    headers=headers,
                    name="groups/join",
                )
                if j.status_code == 201:
                    self.group_id = group_id
                    break

        if not self.group_id:
            return

        sg = self.client.get(f"/api/v1/groups/{self.group_id}/subgroups?size=20", headers=headers, name="groups/subgroups")
        if sg.status_code == 200:
            try:
                items = sg.json().get("data", {}).get("items", [])
                if items:
                    self.subgroup_id = items[0].get("subgroupId")
            except Exception:
                pass

        if self.subgroup_id:
            cr = self.client.get(f"/api/v1/subgroups/{self.subgroup_id}/chat-room", headers=headers, name="subgroups/chat-room")
            if cr.status_code == 200:
                try:
                    self.chat_room_id = cr.json().get("data", {}).get("chatRoomId")
                except Exception:
                    pass

    def _search_group_candidates(self, headers):
        candidate_group_ids = []
        for keyword in GROUP_SEARCH_KEYWORDS:
            res = self.client.post(f"/api/v1/search?keyword={keyword}", headers=headers, name="groups/search")
            if res.status_code != 200:
                continue

            try:
                groups = res.json().get("data", {}).get("groups", [])
            except Exception:
                continue

            for group in groups:
                group_id = group.get("groupId") or group.get("id")
                if group_id and group_id not in candidate_group_ids:
                    candidate_group_ids.append(group_id)

        return candidate_group_ids

    @task(28)
    def browsing_journey(self):
        if not self.token:
            return
        h = self._auth_headers()
        r = self.client.get("/api/v1/main/home?latitude=37.4979&longitude=127.0276", headers=h, name="journey:browsing/main_home")
        rid = self.restaurant_id
        if r.status_code == 200:
            try:
                restaurant_ids = self._extract_restaurant_ids_from_sections(r.json())
                if restaurant_ids:
                    rid = random.choice(restaurant_ids)
                    self.restaurant_id = rid
            except Exception:
                pass
        if not rid:
            return
        self.client.get(f"/api/v1/restaurants/{rid}", headers=h, name="journey:browsing/restaurant_detail")
        self.client.get(f"/api/v1/restaurants/{rid}/menus", headers=h, name="journey:browsing/restaurant_menus")
        self.client.get(f"/api/v1/restaurants/{rid}/reviews", headers=h, name="journey:browsing/restaurant_reviews")

    @task(18)
    def searching_journey(self):
        if not self.token:
            return
        h = self._auth_headers()
        keyword = random.choice(SEARCH_KEYWORDS)
        res = self.client.post(
            f"/api/v1/search?keyword={keyword}&latitude=37.4979&longitude=127.0276&radiusKm=1",
            headers=h,
            name="journey:searching/search",
        )
        rid = self.restaurant_id
        if res.status_code == 200:
            try:
                restaurant_ids = self._extract_restaurant_ids_from_search(res.json())
                if restaurant_ids:
                    rid = random.choice(restaurant_ids)
                    self.restaurant_id = rid
            except Exception:
                pass
        if not rid:
            return
        self.client.get(f"/api/v1/restaurants/{rid}", headers=h, name="journey:searching/restaurant_detail")

    @task(12)
    def group_journey(self):
        if not self.token or not self.group_id:
            return
        h = self._auth_headers()
        self.client.get(f"/api/v1/groups/{self.group_id}", headers=h, name="journey:group/detail")
        self.client.get(f"/api/v1/groups/{self.group_id}/members", headers=h, name="journey:group/members")
        self.client.get(f"/api/v1/groups/{self.group_id}/reviews", headers=h, name="journey:group/reviews")

    @task(12)
    def subgroup_journey(self):
        if not self.token or not self.subgroup_id:
            return
        h = self._auth_headers()
        self.client.get(f"/api/v1/subgroups/{self.subgroup_id}", headers=h, name="journey:subgroup/detail")
        self.client.get(f"/api/v1/subgroups/{self.subgroup_id}/members", headers=h, name="journey:subgroup/members")
        self.client.get(f"/api/v1/subgroups/{self.subgroup_id}/reviews", headers=h, name="journey:subgroup/reviews")

    @task(12)
    def personal_journey(self):
        if not self.token:
            return
        h = self._auth_headers()
        self.client.get("/api/v1/members/me", headers=h, name="journey:personal/me")
        self.client.get("/api/v1/members/me/groups", headers=h, name="journey:personal/groups")
        self.client.get("/api/v1/members/me/favorites/restaurants", headers=h, name="journey:personal/favorites")
        self.client.get("/api/v1/members/me/notifications", headers=h, name="journey:personal/notifications")

    @task(10)
    def chat_journey(self):
        if not self.token or not self.chat_room_id:
            return
        h = self._auth_headers()
        m = self.client.get(f"/api/v1/chat-rooms/{self.chat_room_id}/messages?size=20", headers=h, name="journey:chat/messages")
        self.client.post(
            f"/api/v1/chat-rooms/{self.chat_room_id}/messages",
            json={"messageType": "TEXT", "content": f"locust-{int(time.time() * 1000)}"},
            headers=h,
            name="journey:chat/send",
        )

        if m.status_code == 200:
            try:
                items = m.json().get("data", {}).get("data", [])
                if items:
                    last_id = items[-1].get("id")
                    if last_id:
                        self.client.patch(
                            f"/api/v1/chat-rooms/{self.chat_room_id}/read-cursor",
                            json={"lastReadMessageId": last_id},
                            headers=h,
                            name="journey:chat/read_cursor",
                        )
            except Exception:
                pass

    @task(8)
    def writing_journey(self):
        if not self.token or not self.group_id or not self.restaurant_id:
            return
        h = self._auth_headers()
        self.client.post(
            f"/api/v1/restaurants/{self.restaurant_id}/reviews",
            json={
                "content": f"locust-review-{int(time.time() * 1000)}",
                "groupId": self.group_id,
                "keywordIds": [1],
                "isRecommended": True,
            },
            headers=h,
            name="journey:writing/review",
        )

        self.client.post(
            "/api/v1/members/me/favorites/restaurants",
            json={"restaurantId": self.restaurant_id},
            headers=h,
            name="journey:writing/favorite_add",
        )
        self.client.delete(
            f"/api/v1/members/me/favorites/restaurants/{self.restaurant_id}",
            headers=h,
            name="journey:writing/favorite_del",
        )
