import os
import random
import time
from locust import HttpUser, task, between


def parse_ratio_env(name, default, allow_zero=False):
    try:
        value = float(os.getenv(name, str(default)))
    except ValueError:
        return default
    if allow_zero and value == 0:
        return 0.0
    if value <= 0:
        return default
    if value >= 1:
        return 1.0
    return value


BASE_URL = os.getenv("BASE_URL", "https://stg.tasteam.kr")
TEST_AUTH_TOKEN_PATH = os.getenv("TEST_AUTH_TOKEN_PATH", "/api/v1/test/auth/token")
USER_ID_MAX = int(os.getenv("USER_ID_MAX", "1000"))
TEST_GROUP_CODE = os.getenv("TEST_GROUP_CODE", "LOCAL-1234")
GROUP_SEARCH_KEYWORDS = [
    keyword.strip()
    for keyword in os.getenv("GROUP_SEARCH_KEYWORDS", "테스트").split(",")
    if keyword.strip()
]

SEARCH_VARIATION_KEYWORD_LIMIT = int(os.getenv("SEARCH_VARIATION_KEYWORD_LIMIT", "336"))
SEARCH_TYPING_KEYWORD_SHARE = parse_ratio_env("SEARCH_TYPING_KEYWORD_SHARE", 0.05, allow_zero=True)
SEARCH_RADIUS_KM = [
    float(value.strip())
    for value in os.getenv("SEARCH_RADIUS_KM", "0.4,0.8,1.5,3.0").split(",")
    if value.strip()
]
SEARCH_LOCATION_OFFSETS = []
for raw_offset in os.getenv("SEARCH_LOCATION_OFFSETS", "0.004,0.012").split(","):
    raw_offset = raw_offset.strip()
    if not raw_offset:
        continue
    value = abs(float(raw_offset))
    SEARCH_LOCATION_OFFSETS.extend([-value, value])

SEARCH_KEYWORD_EXACT_TERMS = [
    "파스타", "피자", "치킨", "초밥", "스시", "버거", "카페", "디저트",
    "삼겹살", "쌀국수", "라멘", "곱창", "갈비", "샐러드", "브런치", "한식",
    "비건", "스테이크", "떡볶이", "칼국수", "냉면", "짜장면", "짬뽕", "마라탕",
    "훠궈", "돈까스", "우동", "규카츠", "오마카세", "족발", "보쌈", "순대국",
    "감자탕", "닭갈비", "김치찌개", "부대찌개", "순두부", "비빔밥", "덮밥", "돈부리",
    "텐동", "타코", "퀘사디아", "커리", "인도커리", "베이커리", "와플", "도넛",
    "케이크", "아이스크림", "막창", "양꼬치", "해장국", "국밥", "설렁탕", "샤브샤브",
    "중식", "일식", "양식", "고기집", "술집", "이자카야", "분식", "백반",
    "죽", "국수", "찜닭", "닭한마리", "보양식", "빙수", "포케", "샌드위치",
    "토스트", "파니니", "리조또", "그라탕", "퐁듀", "딤섬", "소바", "해산물",
]
SEARCH_KEYWORD_PARTIAL_TERMS = [
    "파스", "피자집", "치킨집", "초밥집", "스시집", "버거집", "카페추천", "디저트카페",
    "삼겹", "삼겹살집", "쌀국", "라멘집", "곱창집", "갈비집", "샐러", "브런",
    "한식집", "비건식당", "스테", "떡볶", "칼국", "냉면집", "짜장", "짬뽕집",
    "마라", "훠궈집", "돈까", "우동집", "규카", "오마", "족발집", "보쌈집",
    "순대", "감자", "닭갈", "김치", "부대", "순두", "비빔", "덮밥집",
    "돈부", "텐동집", "타코집", "퀘사", "커리집", "베이커", "와플집", "도넛집",
    "케이크집", "아이스", "막창집", "양꼬", "해장", "국밥집", "설렁", "샤브",
    "중식집", "일식집", "양식집", "고깃집", "술집추천", "이자카", "분식집", "백반집",
    "죽집", "국수집", "찜닭집", "닭한", "보양", "빙수집", "포케집", "샌드",
    "토스", "파니", "리조", "그라", "퐁듀집", "딤섬집", "소바집", "해산물집",
]
SEARCH_KEYWORD_REGION_TERMS = [
    "강남맛집", "강남점심", "강남저녁", "강남회식", "강남카페", "강남삼겹", "역삼맛집", "역삼점심",
    "역삼저녁", "선릉맛집", "선릉회식", "잠실맛집", "잠실데이트", "잠실브런치", "성수맛집", "성수카페",
    "성수브런치", "홍대맛집", "홍대술집", "홍대데이트", "합정맛집", "합정카페", "망원맛집", "망원브런치",
    "여의도맛집", "여의도회식", "종로맛집", "종로점심", "명동맛집", "명동디저트", "신사맛집", "신사브런치",
    "압구정맛집", "압구정데이트", "청담맛집", "청담오마카세", "을지로맛집", "을지로술집", "건대맛집", "건대술집",
    "용산맛집", "용산카페", "마포맛집", "마포고기", "서초맛집", "서초점심", "연남맛집", "연남카페",
    "문래맛집", "문래술집", "송리단길맛집", "가로수길맛집", "익선동맛집", "익선동카페", "한남동맛집", "한남동데이트",
    "성신여대맛집", "잠원맛집", "교대맛집", "교대회식", "사당맛집", "사당술집", "영등포맛집", "영등포회식",
    "을지로입구맛집", "서울역맛집", "시청맛집", "광화문맛집", "광화문점심", "왕십리맛집", "왕십리고기", "건대입구맛집",
    "노량진맛집", "신촌맛집", "신촌술집", "대학로맛집", "혜화맛집", "망원동맛집", "성수동맛집", "잠실새내맛집",
]
SEARCH_KEYWORD_CONTEXT_TERMS = [
    "점심맛집", "저녁맛집", "회식장소", "데이트맛집", "혼밥맛집", "가족모임식당", "야식맛집", "퇴근후한잔",
    "주말브런치", "가성비맛집", "조용한식당", "분위기좋은식당", "매운음식", "깔끔한식당", "로컬맛집", "신상맛집",
    "든든한한끼", "웨이팅없는맛집", "프리미엄식당", "늦게까지", "24시간식당", "단체석식당", "예약가능식당", "포장가능",
    "혼술하기좋은곳", "비오는날맛집", "해장맛집", "술안주맛집", "소개팅식당", "기념일레스토랑", "점심식사", "저녁식사",
    "모임장소", "점심회식", "저녁회식", "가성비카페", "조용한카페", "디저트맛집", "브런치카페", "늦은저녁",
    "주차되는식당", "주차가능카페", "애견동반식당", "아이랑가기좋은곳", "부모님모시고갈곳", "직장인점심", "직장인저녁", "친구모임식당",
    "소개팅카페", "기념일코스", "삼겹맛집", "파스타맛집", "초밥맛집", "치킨맛집", "버거맛집", "카페맛집",
    "곱창맛집", "갈비맛집", "마라탕맛집", "라멘맛집", "샐러드맛집", "비건맛집", "브런치맛집", "한식맛집",
    "중식맛집", "일식맛집", "양식맛집", "강남삼", "성수카", "홍대술", "잠실파", "여의도회",
    "명동디", "종로국", "신사브", "압구정오", "청담스", "을지로술", "망원브", "합정카",
    "연남카", "문래술", "광화문점", "서울역점", "시청점", "왕십리고", "건대술", "신촌술",
    "대학로연극맛집", "혜화연극맛집", "삼겹살추천", "파스타추천", "초밥추천", "치킨추천", "버거추천", "디저트추천",
]
SEARCH_KEYWORD_TYPING_TERMS = [
    "삼겹살맛", "삼겹ㅅ", "파스타맛", "파스ㅌ", "초밥맛", "초바ㅂ", "치킨맛", "치키ㄴ",
    "버거맛", "버거ㅈ", "디저트맛", "디저ㅌ", "라멘맛", "라메ㄴ", "마라탕맛", "마라ㅌ",
    "쌀국수맛", "쌀국ㅅ", "곱창맛", "곱차ㅇ", "브런치맛", "브런ㅊ", "샐러드맛", "샐러ㄷ",
    "성수카ㅍ", "성수브ㄹ", "강남맛ㅈ", "강남점ㅅ", "홍대술ㅈ", "홍대데ㅇ", "잠실브ㄹ", "잠실데ㅇ",
    "여의도회ㅅ", "종로점ㅅ", "명동디ㅈ", "신사브ㄹ", "연남카ㅍ", "문래술ㅈ", "합정카ㅍ", "망원브ㄹ",
    "가성비맛ㅈ", "조용한식ㄷ", "웨이팅없ㄴ", "주말브런ㅊ", "퇴근후한ㅈ", "직장인점ㅅ", "소개팅카ㅍ", "기념일레ㅅ",
]
SEARCH_LOCATION_ANCHORS = [
    {"lat": 37.5665, "lon": 126.9780},
    {"lat": 37.4979, "lon": 127.0276},
    {"lat": 37.5563, "lon": 126.9723},
    {"lat": 37.5519, "lon": 126.9918},
    {"lat": 37.5172, "lon": 127.0473},
    {"lat": 37.5144, "lon": 127.1050},
    {"lat": 37.5796, "lon": 126.9770},
    {"lat": 37.5443, "lon": 127.0557},
    {"lat": 37.5600, "lon": 127.0369},
    {"lat": 37.5172, "lon": 127.0391},
    {"lat": 37.5326, "lon": 126.9003},
    {"lat": 37.5400, "lon": 127.0695},
    {"lat": 37.5779, "lon": 126.9849},
    {"lat": 37.5174, "lon": 127.0272},
    {"lat": 37.5229, "lon": 127.0247},
    {"lat": 37.5483, "lon": 126.9164},
    {"lat": 37.5838, "lon": 127.0021},
    {"lat": 37.5506, "lon": 126.9217},
    {"lat": 37.5591, "lon": 126.9264},
    {"lat": 37.5670, "lon": 126.9852},
]


def unique_keywords(values):
    seen = set()
    catalog = []
    for candidate in values:
        if candidate in seen:
            continue
        seen.add(candidate)
        catalog.append(candidate)
    return catalog


def build_search_keyword_catalog():
    catalog = []
    for values in (
        SEARCH_KEYWORD_EXACT_TERMS,
        SEARCH_KEYWORD_PARTIAL_TERMS,
        SEARCH_KEYWORD_REGION_TERMS,
        SEARCH_KEYWORD_CONTEXT_TERMS,
    ):
        catalog.extend(values)
    return unique_keywords(catalog)[:SEARCH_VARIATION_KEYWORD_LIMIT]


def build_search_location_catalog():
    catalog = []
    for anchor in SEARCH_LOCATION_ANCHORS:
        for lat_offset in SEARCH_LOCATION_OFFSETS:
            for lon_offset in SEARCH_LOCATION_OFFSETS:
                catalog.append({
                    "lat": round(anchor["lat"] + lat_offset, 4),
                    "lon": round(anchor["lon"] + lon_offset, 4),
                })
    return catalog


SEARCH_KEYWORDS = build_search_keyword_catalog()
SEARCH_TYPING_KEYWORDS = unique_keywords(SEARCH_KEYWORD_TYPING_TERMS)
SEARCH_ALL_KEYWORDS = unique_keywords(SEARCH_KEYWORDS + SEARCH_TYPING_KEYWORDS)
SEARCH_LOCATIONS = build_search_location_catalog()
SEARCH_VARIATION_COUNT = len(SEARCH_ALL_KEYWORDS) * len(SEARCH_LOCATIONS) * len(SEARCH_RADIUS_KM)


def pick_search_keyword():
    if SEARCH_TYPING_KEYWORDS and random.random() < SEARCH_TYPING_KEYWORD_SHARE:
        return random.choice(SEARCH_TYPING_KEYWORDS)
    return random.choice(SEARCH_KEYWORDS)


class TasteamUser(HttpUser):
    host = BASE_URL
    wait_time = between(1, 5)
    _variation_logged = False

    def on_start(self):
        self.token = None
        self.group_id = None
        self.subgroup_id = None
        self.chat_room_id = None
        self.restaurant_id = None

        if not TasteamUser._variation_logged:
            print(
                f"[locust] search combinations={SEARCH_VARIATION_COUNT} "
                f"({len(SEARCH_ALL_KEYWORDS)} keywords = {len(SEARCH_KEYWORDS)} primary + "
                f"{len(SEARCH_TYPING_KEYWORDS)} typing @ {int(SEARCH_TYPING_KEYWORD_SHARE * 100)}% "
                f"x {len(SEARCH_LOCATIONS)} locations x {len(SEARCH_RADIUS_KM)} radii)"
            )
            TasteamUser._variation_logged = True

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

    def _pick_search_location(self):
        return random.choice(SEARCH_LOCATIONS)

    def _pick_search_radius_km(self):
        return random.choice(SEARCH_RADIUS_KM)

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
        keyword = pick_search_keyword()
        loc = self._pick_search_location()
        radius_km = self._pick_search_radius_km()
        res = self.client.post(
            f"/api/v1/search?keyword={keyword}&latitude={loc['lat']}&longitude={loc['lon']}&radiusKm={radius_km}",
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
