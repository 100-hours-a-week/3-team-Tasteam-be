#!/usr/bin/env python3
"""Tasteam 더미 데이터 SQL 생성기.

앱 서비스 레이어를 거치지 않고, 현재 스키마에 맞는 INSERT SQL 파일을 생성한다.
- 입력: JSON 설정 파일(선택)
- 출력: 실행 가능한 SQL 파일
"""

from __future__ import annotations

import argparse
import copy
import json
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

DEFAULT_CONFIG: dict[str, Any] = {
    "counts": {
        "member_count": 10_000,
        "restaurant_count": 10_000,
        "group_count": 1_500,
        "subgroup_per_group": 4,
        "member_per_group": 30,
        "review_count": 300_000,
        "chat_message_per_room": 167,
        "notification_count": 400_000,
        "favorite_count": 100_000,
        "subgroup_favorite_count": 1_000,
    },
    "content": {
        "run_token": "",
        "member_email_prefix": "dummy",
        "member_email_suffix": "@dummy.tasteam.kr",
        "member_nickname_prefix": "더미유저",
        "profile_image_base_url": "https://cdn.tasteam.kr/profiles",
        "group_logo_base_url": "https://cdn.tasteam.kr/groups",
        "subgroup_profile_base_url": "https://cdn.tasteam.kr/subgroups",
        "menu_image_base_url": "https://cdn.tasteam.kr/menus",
        "group_password_prefix": "grp-pass",
        "subgroup_password_prefix": "sg-pass",
        "member_email_local_first": [
            "min", "ji", "seo", "ha", "do", "yeon", "jun", "tae", "su", "hyeon"
        ],
        "member_email_local_second": [
            "kim", "lee", "park", "choi", "jung", "han", "yoon", "oh", "lim", "kang"
        ],
        "member_email_domains": [
            "gmail.com", "naver.com", "kakao.com", "outlook.com", "tasteam.kr"
        ],
        "member_nickname_adjectives": [
            "맛집헌터", "퇴근러", "점심탐험가", "야식러버", "국밥수집가", "카페순례자"
        ],
        "member_nickname_nouns": [
            "민초", "새우", "곱창", "파스타", "초밥", "버거", "마라", "치킨"
        ],
        "member_intro_sentences": [
            "근처 직장인이라 점심 맛집 자주 찾아요.",
            "주말에는 동네 카페 투어를 즐깁니다.",
            "새로운 메뉴 도전하는 걸 좋아해요.",
            "웨이팅 짧고 회전 빠른 집 선호합니다.",
            "혼밥 가능한 조용한 공간을 찾고 있어요."
        ],
        "restaurant_name_prefix": "더미식당",
        "restaurant_address_prefix": "서울시 강남구 더미로",
        "restaurant_name_left_tokens": [
            "강남", "연남", "성수", "잠실", "광화문", "해운대", "판교", "홍대"
        ],
        "restaurant_name_right_tokens": [
            "화로집", "스시바", "국밥집", "브런치랩", "포차", "카페", "버거하우스", "파스타키친"
        ],
        "group_name_prefix": "더미그룹",
        "group_address_prefix": "서울시 강남구 테스트로",
        "group_topic_tokens": [
            "백엔드", "프론트엔드", "데이터", "AI", "디자인", "PM", "운영", "인프라"
        ],
        "subgroup_name_prefix": "더미팀",
        "subgroup_topic_tokens": [
            "점심", "야식", "회식", "카페", "러닝", "스터디", "프로젝트", "탐방"
        ],
        "chat_message_prefix": "더미 채팅 메시지",
        "review_content_prefix": "더미 리뷰 내용입니다.",
        "notification_title_prefix": "더미 알림 제목",
        "notification_body_prefix": "더미 알림 내용입니다.",
        "chat_message_short_pool": [
            "좋아요", "여기 어때요?", "지금 출발합니다", "메뉴 추천 부탁해요", "오늘 사람 많네요"
        ],
        "chat_message_long_pool": [
            "가게 분위기가 깔끔하고 대화하기 편했어요.",
            "점심 피크에는 웨이팅이 있으니 11시 반 전에 가는 걸 추천해요.",
            "가격대는 조금 있지만 양이 충분해서 만족도가 높았어요.",
            "재방문 의사 있고, 다음에는 다른 메뉴도 시도해보려 해요."
        ],
        "review_positive_pool": [
            "직원 응대가 친절하고 음식이 빨리 나왔습니다.",
            "대표 메뉴의 완성도가 높고 재료 신선도가 좋았습니다.",
            "매장 동선이 편하고 좌석 간격이 넉넉했습니다."
        ],
        "review_negative_pool": [
            "피크타임에는 대기 시간이 길어질 수 있습니다.",
            "일부 메뉴는 간이 조금 강하게 느껴졌습니다.",
            "주차가 다소 불편한 편이었습니다."
        ],
        "review_context_pool": [
            "동료들과 점심 모임으로 방문했습니다.",
            "퇴근 후 소규모 모임으로 다녀왔습니다.",
            "주말 오후에 가족과 함께 이용했습니다.",
            "혼밥으로 방문했는데 부담이 적었습니다."
        ],
        "notification_title_pool": [
            "새 채팅이 도착했어요",
            "즐겨찾기한 식당에 새 리뷰가 올라왔어요",
            "관심 그룹 공지 확인해보세요",
            "지금 인기 많은 맛집을 확인해보세요"
        ],
        "notification_body_pool": [
            "놓치지 않도록 지금 바로 확인해보세요.",
            "최근 반응이 좋아 빠르게 마감될 수 있어요.",
            "새로운 멤버의 추천 리뷰가 등록됐습니다.",
            "설정에서 알림 유형을 자유롭게 조정할 수 있어요."
        ],
        "menu_name_prefix": "더미메뉴",
        "menu_category_names": ["메인", "음료"],
        "notification_types": ["CHAT", "SYSTEM", "NOTICE"],
        "notification_channels": ["WEB", "PUSH", "EMAIL", "SMS"],
        "search_keywords": [
            "한식",
            "중식",
            "일식",
            "양식",
            "분식",
            "카페",
            "치킨",
            "피자",
            "버거",
            "초밥",
            "삼겹살",
            "냉면",
            "국밥",
            "라멘",
            "파스타",
        ],
        "sidos": ["서울시", "경기도", "부산시", "인천시", "대구시"],
        "sigungus": ["강남구", "서초구", "해운대구", "연수구", "수성구"],
        "eupmyeondong": "역삼동",
        "postal_code": "06000",
        "summary_text": "더미 리뷰 요약입니다.",
        "summary_highlights": ["맛있다", "친절하다"],
        "notification_event_prefix": "dummy",
    },
    "tuning": {
        "menu_per_category": 3,
        "base_menu_price": 8_000,
        "max_keywords_per_review": 3,
    },
}

LIMITS = {
    "member_count": 100_000,
    "restaurant_count": 50_000_000,
    "group_count": 5_000,
    "subgroup_per_group": 1_000,
    "member_per_group": 1_000,
    "review_count": 100_000_000,
    "chat_message_per_room": 1_000_000_000,
    "notification_count": 2_000_000,
    "favorite_count": 500_000,
    "subgroup_favorite_count": 500_000,
}


class ConfigError(ValueError):
    """설정 검증 실패."""


def deep_merge(base: dict[str, Any], override: dict[str, Any]) -> dict[str, Any]:
    merged = copy.deepcopy(base)
    for key, value in override.items():
        if isinstance(value, dict) and isinstance(merged.get(key), dict):
            merged[key] = deep_merge(merged[key], value)
        else:
            merged[key] = value
    return merged


def read_config(path: Path | None) -> dict[str, Any]:
    if path is None:
        return copy.deepcopy(DEFAULT_CONFIG)

    with path.open("r", encoding="utf-8") as f:
        user_cfg = json.load(f)

    if not isinstance(user_cfg, dict):
        raise ConfigError("설정 파일 루트는 JSON Object여야 합니다.")

    return deep_merge(DEFAULT_CONFIG, user_cfg)


def require_int(name: str, value: Any, min_value: int = 0) -> int:
    if not isinstance(value, int):
        raise ConfigError(f"{name} 값은 정수여야 합니다. 입력값={value!r}")
    if value < min_value:
        raise ConfigError(f"{name} 값은 {min_value} 이상이어야 합니다. 입력값={value}")
    return value


def require_non_empty_string(name: str, value: Any) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ConfigError(f"{name} 값은 비어있지 않은 문자열이어야 합니다.")
    return value


def require_non_empty_list_of_strings(name: str, value: Any) -> list[str]:
    if not isinstance(value, list) or not value:
        raise ConfigError(f"{name} 값은 비어있지 않은 문자열 배열이어야 합니다.")
    out: list[str] = []
    for item in value:
        if not isinstance(item, str) or not item.strip():
            raise ConfigError(f"{name} 배열에는 비어있지 않은 문자열만 허용됩니다.")
        out.append(item)
    return out


def normalize_and_validate(cfg: dict[str, Any]) -> dict[str, Any]:
    counts = cfg.get("counts")
    content = cfg.get("content")
    tuning = cfg.get("tuning")

    if not isinstance(counts, dict) or not isinstance(content, dict) or not isinstance(tuning, dict):
        raise ConfigError("counts/content/tuning 키는 모두 JSON Object여야 합니다.")

    for field, max_value in LIMITS.items():
        value = require_int(field, counts.get(field), 0)
        if value > max_value:
            raise ConfigError(f"{field} 값이 허용 상한({max_value})을 초과했습니다. 입력값={value}")
        counts[field] = value

    tuning["menu_per_category"] = require_int("menu_per_category", tuning.get("menu_per_category"), 1)
    tuning["base_menu_price"] = require_int("base_menu_price", tuning.get("base_menu_price"), 0)
    tuning["max_keywords_per_review"] = require_int(
        "max_keywords_per_review", tuning.get("max_keywords_per_review"), 1
    )

    for key in [
        "member_email_prefix",
        "member_email_suffix",
        "member_nickname_prefix",
        "profile_image_base_url",
        "group_logo_base_url",
        "subgroup_profile_base_url",
        "menu_image_base_url",
        "group_password_prefix",
        "subgroup_password_prefix",
        "restaurant_name_prefix",
        "restaurant_address_prefix",
        "group_name_prefix",
        "group_address_prefix",
        "subgroup_name_prefix",
        "chat_message_prefix",
        "review_content_prefix",
        "notification_title_prefix",
        "notification_body_prefix",
        "menu_name_prefix",
        "eupmyeondong",
        "postal_code",
        "summary_text",
        "notification_event_prefix",
    ]:
        content[key] = require_non_empty_string(key, content.get(key))

    for key in [
        "member_email_local_first",
        "member_email_local_second",
        "member_email_domains",
        "member_nickname_adjectives",
        "member_nickname_nouns",
        "member_intro_sentences",
        "restaurant_name_left_tokens",
        "restaurant_name_right_tokens",
        "group_topic_tokens",
        "subgroup_topic_tokens",
        "menu_category_names",
        "notification_types",
        "notification_channels",
        "chat_message_short_pool",
        "chat_message_long_pool",
        "review_positive_pool",
        "review_negative_pool",
        "review_context_pool",
        "notification_title_pool",
        "notification_body_pool",
        "search_keywords",
        "sidos",
        "sigungus",
        "summary_highlights",
    ]:
        content[key] = require_non_empty_list_of_strings(key, content.get(key))

    if not isinstance(content.get("run_token"), str):
        raise ConfigError("run_token은 문자열이어야 합니다. 비워두면 자동 생성됩니다.")
    content["run_token"] = content["run_token"].strip() or uuid.uuid4().hex[:12]

    member_count = counts["member_count"]
    restaurant_count = counts["restaurant_count"]
    group_count = counts["group_count"]
    subgroup_per_group = counts["subgroup_per_group"]
    member_per_group = counts["member_per_group"]
    review_count = counts["review_count"]
    chat_message_per_room = counts["chat_message_per_room"]
    notification_count = counts["notification_count"]
    favorite_count = counts["favorite_count"]
    subgroup_favorite_count = counts["subgroup_favorite_count"]

    total_subgroups = group_count * subgroup_per_group
    if total_subgroups > 1_000_000:
        raise ConfigError("group_count * subgroup_per_group 값이 1,000,000을 초과할 수 없습니다.")

    total_chat_messages = total_subgroups * chat_message_per_room
    if total_chat_messages > 20_000_000:
        raise ConfigError("(group_count * subgroup_per_group * chat_message_per_room)는 20,000,000 이하만 허용됩니다.")

    if review_count > 0 and (member_count == 0 or restaurant_count == 0 or group_count == 0):
        raise ConfigError("review_count > 0 인 경우 member_count, restaurant_count, group_count는 모두 1 이상이어야 합니다.")

    if notification_count > 0 and member_count == 0:
        raise ConfigError("notification_count > 0 인 경우 member_count는 1 이상이어야 합니다.")

    if favorite_count > 0 and (member_count == 0 or restaurant_count == 0):
        raise ConfigError("favorite_count > 0 인 경우 member_count와 restaurant_count는 1 이상이어야 합니다.")

    max_favorite = member_count * restaurant_count
    if favorite_count > max_favorite:
        raise ConfigError(
            f"favorite_count({favorite_count})가 최대 조합 수(member_count*restaurant_count={max_favorite})를 초과했습니다."
        )

    if subgroup_favorite_count > 0 and (member_count == 0 or restaurant_count == 0 or total_subgroups == 0):
        raise ConfigError(
            "subgroup_favorite_count > 0 인 경우 member_count, restaurant_count, total_subgroups는 모두 1 이상이어야 합니다."
        )

    max_subgroup_favorite = member_count * restaurant_count * total_subgroups
    if subgroup_favorite_count > max_subgroup_favorite:
        raise ConfigError(
            "subgroup_favorite_count가 최대 조합 수"
            f"(member_count*restaurant_count*total_subgroups={max_subgroup_favorite})를 초과했습니다."
        )

    return cfg


def sql_quote(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def sql_text_array(values: list[str]) -> str:
    return "ARRAY[" + ", ".join(sql_quote(v) for v in values) + "]"


def jsonb_literal(payload: dict[str, Any]) -> str:
    dumped = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    return sql_quote(dumped) + "::jsonb"


def build_seed_sql(cfg: dict[str, Any]) -> str:
    counts = cfg["counts"]
    content = cfg["content"]
    tuning = cfg["tuning"]

    member_count = counts["member_count"]
    restaurant_count = counts["restaurant_count"]
    group_count = counts["group_count"]
    subgroup_per_group = counts["subgroup_per_group"]
    member_per_group = counts["member_per_group"]
    review_count = counts["review_count"]
    chat_message_per_room = counts["chat_message_per_room"]
    notification_count = counts["notification_count"]
    favorite_count = counts["favorite_count"]
    subgroup_favorite_count = counts["subgroup_favorite_count"]

    run_token = content["run_token"]
    menu_per_category = tuning["menu_per_category"]
    base_menu_price = tuning["base_menu_price"]
    max_keywords_per_review = tuning["max_keywords_per_review"]

    total_subgroups = group_count * subgroup_per_group
    members_per_group_effective = min(member_count, member_per_group)
    rows_per_member_pref = len(content["notification_channels"]) * len(content["notification_types"])
    safe_total_subgroups = max(total_subgroups, 1)
    safe_group_count = max(group_count, 1)
    restaurant_hotspot_size = max(1, restaurant_count // 10) if restaurant_count > 0 else 0
    restaurant_tail_size = restaurant_count - restaurant_hotspot_size

    summary_json = jsonb_literal(
        {
            "summary": content["summary_text"],
            "highlights": content["summary_highlights"],
        }
    )

    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S %Z")

    lines: list[str] = []
    a = lines.append

    a("-- Auto-generated dummy seed SQL for Tasteam")
    a(f"-- generated_at: {generated_at}")
    a(f"-- run_token: {run_token}")
    a("-- This script is generated by scripts/dummy_sql_generator/generate_dummy_seed_sql.py")
    a("")
    a("BEGIN;")
    a("SET LOCAL client_min_messages TO WARNING;")
    a("")

    a("CREATE TEMP TABLE tmp_dummy_member (seq INTEGER PRIMARY KEY, id BIGINT NOT NULL) ON COMMIT DROP;")
    a("CREATE TEMP TABLE tmp_dummy_restaurant (seq INTEGER PRIMARY KEY, id BIGINT NOT NULL) ON COMMIT DROP;")
    a("CREATE TEMP TABLE tmp_dummy_group (seq INTEGER PRIMARY KEY, id BIGINT NOT NULL) ON COMMIT DROP;")
    a(
        "CREATE TEMP TABLE tmp_dummy_subgroup ("
        "seq INTEGER PRIMARY KEY, id BIGINT NOT NULL, group_id BIGINT NOT NULL, "
        "group_seq INTEGER NOT NULL, subgroup_index INTEGER NOT NULL"
        ") ON COMMIT DROP;"
    )
    a(
        "CREATE TEMP TABLE tmp_dummy_chat_room ("
        "seq INTEGER PRIMARY KEY, id BIGINT NOT NULL, subgroup_id BIGINT NOT NULL, "
        "group_seq INTEGER NOT NULL, subgroup_index INTEGER NOT NULL"
        ") ON COMMIT DROP;"
    )
    a(
        "CREATE TEMP TABLE tmp_dummy_menu_category ("
        "seq INTEGER PRIMARY KEY, id BIGINT NOT NULL, restaurant_id BIGINT NOT NULL, display_order INTEGER NOT NULL"
        ") ON COMMIT DROP;"
    )
    a(
        "CREATE TEMP TABLE tmp_dummy_review ("
        "seq INTEGER PRIMARY KEY, id BIGINT NOT NULL, member_id BIGINT NOT NULL, "
        "restaurant_id BIGINT NOT NULL, group_id BIGINT NOT NULL, subgroup_id BIGINT, is_recommended BOOLEAN NOT NULL"
        ") ON COMMIT DROP;"
    )
    a("CREATE TEMP TABLE tmp_dummy_keyword (seq INTEGER PRIMARY KEY, id BIGINT NOT NULL) ON COMMIT DROP;")
    a("CREATE TEMP TABLE tmp_dummy_food_category (seq INTEGER PRIMARY KEY, id BIGINT NOT NULL) ON COMMIT DROP;")
    a("")

    a("INSERT INTO tmp_dummy_keyword (seq, id)")
    a("SELECT ROW_NUMBER() OVER (ORDER BY id), id")
    a("FROM keyword")
    a("ORDER BY id;")
    a("")
    a("INSERT INTO tmp_dummy_food_category (seq, id)")
    a("SELECT ROW_NUMBER() OVER (ORDER BY id), id")
    a("FROM food_category")
    a("ORDER BY id;")
    a("")

    if member_count > 0:
        a("-- Members")
        a("INSERT INTO tmp_dummy_member (seq, id)")
        a(f"SELECT gs, nextval('member_seq')")
        a(f"FROM generate_series(1, {member_count}) AS gs;")
        a("")

        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(content['member_email_local_first'])}::TEXT[] AS email_a,")
        a(f"         {sql_text_array(content['member_email_local_second'])}::TEXT[] AS email_b,")
        a(f"         {sql_text_array(content['member_email_domains'])}::TEXT[] AS email_domains,")
        a(f"         {sql_text_array(content['member_nickname_adjectives'])}::TEXT[] AS nick_adj,")
        a(f"         {sql_text_array(content['member_nickname_nouns'])}::TEXT[] AS nick_noun,")
        a(f"         {sql_text_array(content['member_intro_sentences'])}::TEXT[] AS intro_lines")
        a(")")
        a(
            "INSERT INTO member "
            "(id, email, nickname, status, role, profile_image_url, introduction, "
            "last_login_at, agreed_terms_at, agreed_privacy_at, created_at, updated_at)"
        )
        a("SELECT")
        a("  m.id,")
        a(
            "  LOWER("
            + sql_quote(content["member_email_prefix"])
            + " || '-' || "
            + sql_quote(run_token)
            + " || '-' || m.seq || '+' || "
            + "cfg.email_a[((m.seq - 1) % CARDINALITY(cfg.email_a)) + 1] || '.' || "
            + "cfg.email_b[((m.seq - 1) % CARDINALITY(cfg.email_b)) + 1] || '@' || "
            + "cfg.email_domains[((m.seq - 1) % CARDINALITY(cfg.email_domains)) + 1]"
            + "),"
        )
        a(
            "  CASE"
            "    WHEN MOD(m.seq, 3) = 0 THEN "
            + sql_quote(content["member_nickname_prefix"])
            + " || '-' || LPAD(m.seq::TEXT, 4, '0') "
            "    WHEN MOD(m.seq, 3) = 1 THEN "
            "cfg.nick_adj[((m.seq - 1) % CARDINALITY(cfg.nick_adj)) + 1] || "
            "cfg.nick_noun[((m.seq - 1) % CARDINALITY(cfg.nick_noun)) + 1] || m.seq "
            "    ELSE "
            "cfg.nick_noun[((m.seq - 1) % CARDINALITY(cfg.nick_noun)) + 1] || '-' || "
            "cfg.nick_adj[((m.seq - 1) % CARDINALITY(cfg.nick_adj)) + 1] || '-' || (m.seq % 100) "
            "  END,"
        )
        a("  'ACTIVE',")
        a("  'USER',")
        a(
            "  CASE WHEN MOD(m.seq, 4) = 0 THEN NULL ELSE "
            + sql_quote(content["profile_image_base_url"])
            + " || '/' || "
            + sql_quote(run_token)
            + " || '/' || m.id || '.webp' END,"
        )
        a(
            "  CASE "
            "    WHEN MOD(m.seq, 6) = 0 THEN NULL "
            "    WHEN MOD(m.seq, 2) = 0 THEN "
            "cfg.intro_lines[((m.seq - 1) % CARDINALITY(cfg.intro_lines)) + 1] "
            "    ELSE "
            "cfg.intro_lines[((m.seq - 1) % CARDINALITY(cfg.intro_lines)) + 1] || ' ' || "
            "cfg.intro_lines[((m.seq) % CARDINALITY(cfg.intro_lines)) + 1] "
            "  END,"
        )
        a("  NOW() - ((m.seq % 90) || ' days')::INTERVAL - ((m.seq % 24) || ' hours')::INTERVAL,")
        a("  NOW() - ((m.seq % 180) || ' days')::INTERVAL,")
        a("  NOW() - ((m.seq % 180) || ' days')::INTERVAL + INTERVAL '10 minutes',")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_member m")
        a("CROSS JOIN cfg")
        a("ORDER BY m.seq;")
        a("")

        a("-- Member notification preferences")
        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(content['notification_channels'])}::TEXT[] AS channels,")
        a(f"         {sql_text_array(content['notification_types'])}::TEXT[] AS notif_types")
        a(")")
        a(
            "INSERT INTO member_notification_preference "
            "(member_id, channel, notification_type, is_enabled, created_at, updated_at)"
        )
        a("SELECT")
        a("  m.id,")
        a("  ch.channel,")
        a("  nt.notif_type,")
        a(
            "  ((((m.seq - 1) * "
            + str(rows_per_member_pref)
            + ") + ((ch.channel_ord - 1) * "
            + str(len(content["notification_types"]))
            + ") + nt.type_ord) % 5) <> 0,"
        )
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_member m")
        a("CROSS JOIN cfg")
        a("CROSS JOIN UNNEST(cfg.channels) WITH ORDINALITY AS ch(channel, channel_ord)")
        a("CROSS JOIN UNNEST(cfg.notif_types) WITH ORDINALITY AS nt(notif_type, type_ord)")
        a("ON CONFLICT DO NOTHING;")
        a("")

        a("-- Member search histories")
        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(content['search_keywords'])}::TEXT[] AS keywords")
        a(")")
        a("INSERT INTO member_search_history (member_id, keyword, count, created_at, updated_at)")
        a("SELECT")
        a("  m.id,")
        a("  cfg.keywords[(((m.seq - 1) + kw.off) % CARDINALITY(cfg.keywords)) + 1],")
        a("  1,")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_member m")
        a("CROSS JOIN cfg")
        a("JOIN generate_series(0, 3) AS kw(off)")
        a("  ON kw.off < (2 + ((m.seq - 1) % 4))")
        a("ON CONFLICT DO NOTHING;")
        a("")

    if restaurant_count > 0:
        a("-- Restaurants")
        a("INSERT INTO tmp_dummy_restaurant (seq, id)")
        a("SELECT gs, nextval(pg_get_serial_sequence('restaurant', 'id'))")
        a(f"FROM generate_series(1, {restaurant_count}) AS gs;")
        a("")

        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(content['restaurant_name_left_tokens'])}::TEXT[] AS left_tokens,")
        a(f"         {sql_text_array(content['restaurant_name_right_tokens'])}::TEXT[] AS right_tokens,")
        a(f"         {sql_text_array(content['sidos'])}::TEXT[] AS sidos,")
        a(f"         {sql_text_array(content['sigungus'])}::TEXT[] AS sigungus")
        a(")")
        a(
            "INSERT INTO restaurant "
            "(id, name, full_address, location, phone_number, vector_epoch, created_at, updated_at)"
        )
        a("SELECT")
        a("  r.id,")
        a(
            "  "
            + sql_quote(content["restaurant_name_prefix"])
            + " || '-' || "
            + sql_quote(run_token)
            + " || '-' || "
            + "cfg.left_tokens[((r.seq - 1) % CARDINALITY(cfg.left_tokens)) + 1] || ' ' || "
            + "cfg.right_tokens[((r.seq - 1) % CARDINALITY(cfg.right_tokens)) + 1] || ' ' || "
            + "(MOD(r.seq, 20) + 1) || '호',"
        )
        a(
            "  cfg.sidos[((r.seq - 1) % CARDINALITY(cfg.sidos)) + 1] || ' ' || "
            "cfg.sigungus[((r.seq - 1) % CARDINALITY(cfg.sigungus)) + 1] || ' ' || "
            + sql_quote(content["eupmyeondong"])
            + " || ' ' || (MOD(r.seq, 300) + 1) || '-' || (MOD(r.seq * 7, 50) + 1),"
        )
        a(
            "  ST_SetSRID(ST_MakePoint(126.0 + MOD((r.seq * 37)::BIGINT, 3600) / 1000.0, "
            "33.1 + MOD((r.seq * 53)::BIGINT, 5500) / 1000.0), 4326),"
        )
        a("  '0' || (2 + MOD(r.seq, 8)) || '-' || LPAD((1000 + MOD(r.seq * 31, 9000))::TEXT, 4, '0') || '-' || LPAD((1000 + MOD(r.seq * 17, 9000))::TEXT, 4, '0'),")
        a("  0,")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_restaurant r")
        a("CROSS JOIN cfg")
        a("ORDER BY r.seq;")
        a("")

        a("-- Restaurant address (1:1)")
        a("WITH cfg AS (")
        a(
            f"  SELECT {sql_text_array(content['sidos'])}::TEXT[] AS sidos,"
            f" {sql_text_array(content['sigungus'])}::TEXT[] AS sigungus"
        )
        a(")")
        a(
            "INSERT INTO restaurant_address "
            "(restaurant_id, sido, sigungu, eupmyeondong, postal_code, created_at, updated_at)"
        )
        a("SELECT")
        a("  r.id,")
        a("  cfg.sidos[((r.seq - 1) % CARDINALITY(cfg.sidos)) + 1],")
        a("  cfg.sigungus[((r.seq - 1) % CARDINALITY(cfg.sigungus)) + 1],")
        a("  " + sql_quote(content["eupmyeondong"]) + ",")
        a("  " + sql_quote(content["postal_code"]) + ",")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_restaurant r")
        a("CROSS JOIN cfg")
        a("ORDER BY r.seq;")
        a("")

        a("-- Restaurant weekly schedule (7 rows per restaurant)")
        a(
            "INSERT INTO restaurant_weekly_schedule "
            "(restaurant_id, day_of_week, open_time, close_time, is_closed, effective_from, effective_to, created_at, updated_at)"
        )
        a("SELECT")
        a("  r.id,")
        a("  d.day_of_week,")
        a("  CASE WHEN d.day_of_week = 7 THEN NULL ELSE TIME '09:00:00' END,")
        a("  CASE WHEN d.day_of_week = 7 THEN NULL ELSE TIME '21:00:00' END,")
        a("  (d.day_of_week = 7),")
        a("  NULL,")
        a("  NULL,")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_restaurant r")
        a("CROSS JOIN generate_series(1, 7) AS d(day_of_week)")
        a("ORDER BY r.seq, d.day_of_week;")
        a("")

        a("-- Restaurant food categories (food_category가 비어있으면 skip)")
        a("DO $$")
        a("DECLARE")
        a("  category_count INTEGER;")
        a("BEGIN")
        a("  SELECT COUNT(*) INTO category_count FROM tmp_dummy_food_category;")
        a("  IF category_count > 0 THEN")
        a("    INSERT INTO restaurant_food_category (restaurant_id, food_category_id)")
        a("    SELECT")
        a("      r.id,")
        a("      fc.id")
        a("    FROM tmp_dummy_restaurant r")
        a("    JOIN generate_series(0, 1) AS c(off)")
        a("      ON c.off < (1 + ((r.seq - 1) % 2))")
        a("    JOIN tmp_dummy_food_category fc")
        a("      ON fc.seq = ((((r.seq - 1) + c.off) % category_count) + 1)")
        a("    ON CONFLICT DO NOTHING;")
        a("  END IF;")
        a("END $$;")
        a("")

        a("-- Menu categories")
        a("INSERT INTO tmp_dummy_menu_category (seq, id, restaurant_id, display_order)")
        a("SELECT")
        a("  ROW_NUMBER() OVER (ORDER BY r.seq, c.idx),")
        a("  nextval(pg_get_serial_sequence('menu_category', 'id')),")
        a("  r.id,")
        a("  c.idx - 1")
        a("FROM tmp_dummy_restaurant r")
        a(f"CROSS JOIN generate_series(1, {len(content['menu_category_names'])}) AS c(idx)")
        a("ORDER BY r.seq, c.idx;")
        a("")

        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(content['menu_category_names'])}::TEXT[] AS category_names")
        a(")")
        a("INSERT INTO menu_category (id, restaurant_id, name, display_order, created_at, updated_at)")
        a("SELECT")
        a("  mc.id,")
        a("  mc.restaurant_id,")
        a("  cfg.category_names[mc.display_order + 1],")
        a("  mc.display_order,")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_menu_category mc")
        a("CROSS JOIN cfg")
        a("ORDER BY mc.seq;")
        a("")

        a("-- Menus")
        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(content['restaurant_name_right_tokens'])}::TEXT[] AS menu_tokens")
        a(")")
        a(
            "INSERT INTO menu "
            "(category_id, name, description, price, image_url, is_recommended, display_order, created_at, updated_at)"
        )
        a("SELECT")
        a("  mc.id,")
        a(
            "  "
            + sql_quote(content["menu_name_prefix"])
            + " || '-' || cfg.menu_tokens[((mc.seq - 1) % CARDINALITY(cfg.menu_tokens)) + 1] || '-' || (((mc.seq - 1) * "
            + str(menu_per_category)
            + ") + m.menu_idx),"
        )
        a(
            "  CASE "
            "    WHEN MOD(((mc.seq - 1) * "
            + str(menu_per_category)
            + ") + m.menu_idx, 3) = 0 THEN "
            "'대표 메뉴입니다.' "
            "    ELSE "
            "'재료 본연의 풍미를 살린 메뉴입니다. 추천 조합으로 함께 즐겨보세요.' "
            "  END,"
        )
        a(
            "  (MOD(((mc.seq - 1) * "
            + str(menu_per_category)
            + ") + (m.menu_idx - 1), 5) * 1000) + "
            + str(base_menu_price)
            + ","
        )
        a(
            "  CASE WHEN MOD(((mc.seq - 1) * "
            + str(menu_per_category)
            + ") + m.menu_idx, 4) = 0 THEN NULL ELSE "
            + sql_quote(content["menu_image_base_url"])
            + " || '/' || "
            + sql_quote(run_token)
            + " || '/' || (((mc.seq - 1) * "
            + str(menu_per_category)
            + ") + m.menu_idx) || '.jpg' END,"
        )
        a(
            "  (MOD(((mc.seq - 1) * "
            + str(menu_per_category)
            + ") + (m.menu_idx - 1), 3) = 0),"
        )
        a("  m.menu_idx - 1,")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_menu_category mc")
        a("CROSS JOIN cfg")
        a(f"CROSS JOIN generate_series(1, {menu_per_category}) AS m(menu_idx)")
        a("ORDER BY mc.seq, m.menu_idx;")
        a("")

    if group_count > 0:
        a("-- Groups")
        a("INSERT INTO tmp_dummy_group (seq, id)")
        a("SELECT gs, nextval('group_seq')")
        a(f"FROM generate_series(1, {group_count}) AS gs;")
        a("")

        a(
            "INSERT INTO \"group\" "
            "(id, name, type, logo_image_url, address, detail_address, location, join_type, email_domain, status, created_at, updated_at)"
        )
        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(content['group_topic_tokens'])}::TEXT[] AS topics,")
        a(f"         {sql_text_array(content['sidos'])}::TEXT[] AS sidos,")
        a(f"         {sql_text_array(content['sigungus'])}::TEXT[] AS sigungus")
        a(")")
        a("SELECT")
        a("  g.id,")
        a(
            "  "
            + sql_quote(content["group_name_prefix"])
            + " || '-' || "
            + sql_quote(run_token)
            + " || '-' || cfg.topics[((g.seq - 1) % CARDINALITY(cfg.topics)) + 1] || '-' || g.seq,"
        )
        a("  CASE WHEN MOD(g.seq, 5) = 0 THEN 'OFFICIAL' ELSE 'UNOFFICIAL' END,")
        a(
            "  CASE WHEN MOD(g.seq, 3) = 0 THEN NULL ELSE "
            + sql_quote(content["group_logo_base_url"])
            + " || '/' || "
            + sql_quote(run_token)
            + " || '/' || g.id || '.png' END,"
        )
        a(
            "  cfg.sidos[((g.seq - 1) % CARDINALITY(cfg.sidos)) + 1] || ' ' || "
            "cfg.sigungus[((g.seq - 1) % CARDINALITY(cfg.sigungus)) + 1] || ' ' || "
            + sql_quote(content["group_address_prefix"])
            + " || ' ' || g.seq,"
        )
        a("  '빌딩 ' || (MOD(g.seq, 40) + 1) || '층',")
        a(
            "  ST_SetSRID(ST_MakePoint(126.0 + MOD((g.seq * 41)::BIGINT, 3600) / 1000.0, "
            "33.1 + MOD((g.seq * 59)::BIGINT, 5500) / 1000.0), 4326),"
        )
        a("  CASE WHEN MOD(g.seq, 4) = 0 THEN 'EMAIL' ELSE 'PASSWORD' END,")
        a(
            "  CASE WHEN MOD(g.seq, 4) = 0 THEN "
            "'team' || g.seq || '.tasteam.dev' ELSE NULL END,"
        )
        a("  'ACTIVE',")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_group g")
        a("CROSS JOIN cfg")
        a("ORDER BY g.seq;")
        a("")

    if group_count > 0 and subgroup_per_group > 0:
        a("-- Subgroups")
        a("INSERT INTO tmp_dummy_subgroup (seq, id, group_id, group_seq, subgroup_index)")
        a("SELECT")
        a("  ROW_NUMBER() OVER (ORDER BY g.seq, sg.subgroup_idx),")
        a("  nextval('subgroup_seq'),")
        a("  g.id,")
        a("  g.seq,")
        a("  sg.subgroup_idx")
        a("FROM tmp_dummy_group g")
        a(f"CROSS JOIN generate_series(1, {subgroup_per_group}) AS sg(subgroup_idx)")
        a("ORDER BY g.seq, sg.subgroup_idx;")
        a("")

        a(
            "INSERT INTO subgroup "
            "(id, group_id, name, description, profile_image_url, join_type, join_password, status, member_count, created_at, updated_at)"
        )
        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(content['subgroup_topic_tokens'])}::TEXT[] AS topics")
        a(")")
        a("SELECT")
        a("  sg.id,")
        a("  sg.group_id,")
        a(
            "  "
            + sql_quote(content["subgroup_name_prefix"])
            + " || '-' || cfg.topics[((sg.seq - 1) % CARDINALITY(cfg.topics)) + 1] || '-' || sg.group_seq || '-' || sg.subgroup_index,"
        )
        a("  '관심사 기반 소규모 모임입니다. 일정 조율 후 함께 방문해요.',")
        a(
            "  CASE WHEN MOD(sg.seq, 3) = 0 THEN NULL ELSE "
            + sql_quote(content["subgroup_profile_base_url"])
            + " || '/' || "
            + sql_quote(run_token)
            + " || '/' || sg.id || '.jpg' END,"
        )
        a("  CASE WHEN MOD(sg.seq, 5) = 0 THEN 'PASSWORD' ELSE 'OPEN' END,")
        a(
            "  CASE WHEN MOD(sg.seq, 5) = 0 THEN "
            + sql_quote(content["subgroup_password_prefix"])
            + " || '-' || sg.group_seq || '-' || sg.subgroup_index ELSE NULL END,"
        )
        a("  'ACTIVE',")
        a("  0,")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_subgroup sg")
        a("CROSS JOIN cfg")
        a("ORDER BY sg.seq;")
        a("")

        a("-- Chat rooms (subgroup 1:1)")
        a("INSERT INTO tmp_dummy_chat_room (seq, id, subgroup_id, group_seq, subgroup_index)")
        a("SELECT")
        a("  sg.seq,")
        a("  nextval('chat_room_id_seq'),")
        a("  sg.id,")
        a("  sg.group_seq,")
        a("  sg.subgroup_index")
        a("FROM tmp_dummy_subgroup sg")
        a("ORDER BY sg.seq;")
        a("")

        a("INSERT INTO chat_room (id, subgroup_id, created_at)")
        a("SELECT id, subgroup_id, NOW()")
        a("FROM tmp_dummy_chat_room")
        a("ORDER BY seq;")
        a("")

    if group_count > 0 and members_per_group_effective > 0 and member_count > 0:
        a("-- Group members")
        a("INSERT INTO group_member (id, group_id, member_id, created_at)")
        a("SELECT")
        a("  nextval('group_member_seq'),")
        a("  g.id,")
        a("  m.id,")
        a("  NOW()")
        a("FROM tmp_dummy_group g")
        a(f"JOIN generate_series(0, {members_per_group_effective - 1}) AS off(member_offset) ON TRUE")
        a("JOIN tmp_dummy_member m")
        a(
            "  ON m.seq = (((((g.seq * 37) % "
            + str(member_count)
            + ") + off.member_offset) % "
            + str(member_count)
            + ") + 1)"
        )
        a("ON CONFLICT DO NOTHING;")
        a("")

    if total_subgroups > 0 and members_per_group_effective > 0 and member_count > 0:
        a("-- Subgroup members")
        a("INSERT INTO subgroup_member (id, subgroup_id, member_id, created_at)")
        a("SELECT")
        a("  nextval('subgroup_member_seq'),")
        a("  sg.id,")
        a("  m.id,")
        a("  NOW()")
        a("FROM tmp_dummy_subgroup sg")
        a(f"JOIN generate_series(0, {members_per_group_effective - 1}) AS off(member_offset) ON TRUE")
        a("JOIN tmp_dummy_member m")
        a(
            "  ON m.seq = (("
            "((sg.group_seq * 37) % "
            + str(member_count)
            + ") + "
            "((((sg.group_seq * 31 + sg.subgroup_index * 17) % "
            + str(members_per_group_effective)
            + ") + off.member_offset) % "
            + str(members_per_group_effective)
            + ")"
            ") % "
            + str(member_count)
            + ") + 1"
        )
        a("ON CONFLICT DO NOTHING;")
        a("")

        a("-- Chat room members")
        a("INSERT INTO chat_room_member (member_id, chat_room_id, created_at, updated_at)")
        a("SELECT")
        a("  m.id,")
        a("  cr.id,")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_chat_room cr")
        a("JOIN tmp_dummy_subgroup sg ON sg.id = cr.subgroup_id")
        a(f"JOIN generate_series(0, {members_per_group_effective - 1}) AS off(member_offset) ON TRUE")
        a("JOIN tmp_dummy_member m")
        a(
            "  ON m.seq = (("
            "((sg.group_seq * 37) % "
            + str(member_count)
            + ") + "
            "((((sg.group_seq * 31 + sg.subgroup_index * 17) % "
            + str(members_per_group_effective)
            + ") + off.member_offset) % "
            + str(members_per_group_effective)
            + ")"
            ") % "
            + str(member_count)
            + ") + 1"
        )
        a("ON CONFLICT DO NOTHING;")
        a("")

        a("UPDATE subgroup")
        a(f"SET member_count = {members_per_group_effective}")
        a("WHERE id IN (SELECT id FROM tmp_dummy_subgroup);")
        a("")

    if total_subgroups > 0 and chat_message_per_room > 0 and members_per_group_effective > 0 and member_count > 0:
        a("-- Chat messages")
        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(content['chat_message_short_pool'])}::TEXT[] AS short_msgs,")
        a(f"         {sql_text_array(content['chat_message_long_pool'])}::TEXT[] AS long_msgs")
        a(")")
        a("INSERT INTO chat_message (chat_room_id, member_id, type, content, created_at)")
        a("SELECT")
        a("  cr.id,")
        a("  m.id,")
        a("  'TEXT',")
        a(
            "  CASE "
            "    WHEN MOD(msg.message_idx, 5) IN (0, 1) THEN "
            + sql_quote(content["chat_message_prefix"])
            + " || ' #' || (((cr.seq - 1) * "
            + str(chat_message_per_room)
            + ") + msg.message_idx) || ' ' || "
            "cfg.short_msgs[((msg.message_idx - 1) % CARDINALITY(cfg.short_msgs)) + 1] "
            "    ELSE "
            + sql_quote(content["chat_message_prefix"])
            + " || ' #' || (((cr.seq - 1) * "
            + str(chat_message_per_room)
            + ") + msg.message_idx) || ' ' || "
            "cfg.long_msgs[((msg.message_idx - 1) % CARDINALITY(cfg.long_msgs)) + 1] "
            "  END,"
        )
        a("  NOW()")
        a("FROM tmp_dummy_chat_room cr")
        a("CROSS JOIN cfg")
        a("JOIN tmp_dummy_subgroup sg ON sg.id = cr.subgroup_id")
        a(f"JOIN generate_series(1, {chat_message_per_room}) AS msg(message_idx) ON TRUE")
        a("JOIN tmp_dummy_member m")
        a(
            "  ON m.seq = (("
            "((sg.group_seq * 37) % "
            + str(member_count)
            + ") + "
            "(("
            "((sg.group_seq * 31 + sg.subgroup_index * 17) % "
            + str(members_per_group_effective)
            + ") + ((msg.message_idx - 1) % "
            + str(members_per_group_effective)
            + ")"
            ") % "
            + str(members_per_group_effective)
            + ")"
            ") % "
            + str(member_count)
            + ") + 1"
        )
        a("ORDER BY cr.seq, msg.message_idx;")
        a("")

        a("-- Update last_read_message_id for 70% of chat_room_member rows")
        a("UPDATE chat_room_member crm")
        a("SET last_read_message_id = latest.max_id")
        a("FROM (")
        a("  SELECT chat_room_id, MAX(id) AS max_id")
        a("  FROM chat_message")
        a("  WHERE chat_room_id IN (SELECT id FROM tmp_dummy_chat_room)")
        a("  GROUP BY chat_room_id")
        a(") AS latest")
        a("WHERE crm.chat_room_id = latest.chat_room_id")
        a("  AND (crm.id % 10) < 7;")
        a("")

    if review_count > 0:
        a("-- Reviews")
        a("INSERT INTO tmp_dummy_review (seq, id, member_id, restaurant_id, group_id, subgroup_id, is_recommended)")
        a("SELECT")
        a("  rv.seq,")
        a("  nextval(pg_get_serial_sequence('review', 'id')),")
        a("  m.id,")
        a("  rest.id,")
        a("  g.id,")
        if subgroup_per_group > 0:
            a("  sg.id,")
        else:
            a("  NULL,")
        a("  (MOD(rv.seq - 1, 2) = 0)")
        a(f"FROM generate_series(1, {review_count}) AS rv(seq)")
        a("JOIN tmp_dummy_member m")
        a(f"  ON m.seq = (((rv.seq - 1) % {member_count}) + 1)")
        a("JOIN tmp_dummy_group g")
        a(f"  ON g.seq = (((rv.seq - 1) % {group_count}) + 1)")
        if subgroup_per_group > 0:
            a("LEFT JOIN tmp_dummy_subgroup sg")
            a(
                f"  ON sg.group_id = g.id "
                f"AND sg.subgroup_index = (((rv.seq - 1) % {subgroup_per_group}) + 1)"
            )
        a("JOIN tmp_dummy_restaurant rest")
        if restaurant_tail_size <= 0:
            a(
                f"  ON rest.seq = (CASE WHEN MOD(rv.seq - 1, 10) < 6 "
                f"THEN ((rv.seq - 1) % {restaurant_hotspot_size}) + 1 "
                f"ELSE ((rv.seq - 1) % {restaurant_count}) + 1 END)"
            )
        else:
            a(
                f"  ON rest.seq = (CASE WHEN MOD(rv.seq - 1, 10) < 6 "
                f"THEN ((rv.seq - 1) % {restaurant_hotspot_size}) + 1 "
                f"ELSE {restaurant_hotspot_size} + (((rv.seq - 1) % {restaurant_tail_size}) + 1) END)"
            )
        a("ORDER BY rv.seq;")
        a("")

        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(content['review_positive_pool'])}::TEXT[] AS positive_lines,")
        a(f"         {sql_text_array(content['review_negative_pool'])}::TEXT[] AS negative_lines,")
        a(f"         {sql_text_array(content['review_context_pool'])}::TEXT[] AS context_lines")
        a(")")
        a(
            "INSERT INTO review "
            "(id, member_id, restaurant_id, group_id, subgroup_id, is_recommended, content, created_at, updated_at)"
        )
        a("SELECT")
        a("  rv.id,")
        a("  rv.member_id,")
        a("  rv.restaurant_id,")
        a("  rv.group_id,")
        a("  rv.subgroup_id,")
        a("  rv.is_recommended,")
        a(
            "  CASE "
            "    WHEN MOD(rv.seq, 4) = 0 THEN "
            + sql_quote(content["review_content_prefix"])
            + " || ' ' || cfg.context_lines[((rv.seq - 1) % CARDINALITY(cfg.context_lines)) + 1] || ' ' || "
            "cfg.positive_lines[((rv.seq - 1) % CARDINALITY(cfg.positive_lines)) + 1] || ' ' || "
            "cfg.negative_lines[((rv.seq - 1) % CARDINALITY(cfg.negative_lines)) + 1] "
            "    WHEN MOD(rv.seq, 4) = 1 THEN "
            "cfg.context_lines[((rv.seq - 1) % CARDINALITY(cfg.context_lines)) + 1] || ' ' || "
            "cfg.positive_lines[((rv.seq - 1) % CARDINALITY(cfg.positive_lines)) + 1] "
            "    WHEN MOD(rv.seq, 4) = 2 THEN "
            + sql_quote(content["review_content_prefix"])
            + " || ' ' || cfg.negative_lines[((rv.seq - 1) % CARDINALITY(cfg.negative_lines)) + 1] "
            "    ELSE "
            + sql_quote(content["review_content_prefix"])
            + " || ' 방문 ' || rv.seq || '회차 리뷰입니다.' "
            "  END,"
        )
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_review rv")
        a("CROSS JOIN cfg")
        a("ORDER BY rv.seq;")
        a("")

        a("-- Review keywords (keyword 테이블이 비어있으면 skip)")
        a("DO $$")
        a("DECLARE")
        a("  keyword_count INTEGER;")
        a("  max_per_review INTEGER;")
        a("BEGIN")
        a("  SELECT COUNT(*) INTO keyword_count FROM tmp_dummy_keyword;")
        a("  IF keyword_count > 0 THEN")
        a(f"    max_per_review := LEAST({max_keywords_per_review}, keyword_count);")
        a("    INSERT INTO review_keyword (review_id, keyword_id)")
        a("    SELECT")
        a("      rv.id,")
        a("      kw.id")
        a("    FROM tmp_dummy_review rv")
        a("    JOIN generate_series(0, max_per_review - 1) AS offs(off)")
        a("      ON offs.off < (1 + ((rv.seq - 1) % max_per_review))")
        a("    JOIN tmp_dummy_keyword kw")
        a("      ON kw.seq = ((((rv.seq - 1) % keyword_count) + offs.off) % keyword_count) + 1")
        a("    ON CONFLICT DO NOTHING;")
        a("  END IF;")
        a("END $$;")
        a("")

    if restaurant_count > 0:
        a("-- Restaurant review analysis")
        a(
            "INSERT INTO restaurant_review_sentiment "
            "(restaurant_id, vector_epoch, model_version, positive_count, negative_count, neutral_count, "
            "positive_percent, negative_percent, neutral_percent, analyzed_at)"
        )
        a("SELECT")
        a("  r.id,")
        a("  0,")
        a("  'dummy-v1',")
        a("  ((r.seq - 1) % 10) + 1,")
        a("  2,")
        a("  1,")
        a("  60,")
        a("  20,")
        a("  20,")
        a("  NOW()")
        a("FROM tmp_dummy_restaurant r")
        a("ON CONFLICT (restaurant_id) DO NOTHING;")
        a("")

        a(
            "INSERT INTO restaurant_review_summary "
            "(restaurant_id, vector_epoch, model_version, summary_json, analyzed_at)"
        )
        a("SELECT")
        a("  r.id,")
        a("  0,")
        a("  'dummy-v1',")
        a("  " + summary_json + ",")
        a("  NOW()")
        a("FROM tmp_dummy_restaurant r")
        a("ON CONFLICT DO NOTHING;")
        a("")

    if notification_count > 0:
        a("-- Notifications")
        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(content['notification_types'])}::TEXT[] AS notif_types,")
        a(f"         {sql_text_array(content['notification_title_pool'])}::TEXT[] AS title_pool,")
        a(f"         {sql_text_array(content['notification_body_pool'])}::TEXT[] AS body_pool")
        a(")")
        a(
            "INSERT INTO notification "
            "(member_id, notification_type, title, body, deep_link, event_id, created_at)"
        )
        a("SELECT")
        a("  m.id,")
        a("  cfg.notif_types[(MOD(ns.seq, CARDINALITY(cfg.notif_types)) + 1)],")
        a(
            "  "
            + sql_quote(content["notification_title_prefix"])
            + " || ' · ' || cfg.title_pool[((ns.seq) % CARDINALITY(cfg.title_pool)) + 1],"
        )
        a(
            "  "
            + sql_quote(content["notification_body_prefix"])
            + " || ' ' || cfg.body_pool[((ns.seq) % CARDINALITY(cfg.body_pool)) + 1] || "
            "' (ref:' || (ns.seq + 1) || ')',"
        )
        a(
            "  CASE cfg.notif_types[(MOD(ns.seq, CARDINALITY(cfg.notif_types)) + 1)] "
            "    WHEN 'CHAT' THEN '/chat/rooms/' || ((ns.seq % "
            + str(safe_total_subgroups)
            + ") + 1) "
            "    WHEN 'NOTICE' THEN '/groups/' || ((ns.seq % "
            + str(safe_group_count)
            + ") + 1) "
            "    ELSE '/notifications' "
            "  END,"
        )
        a(
            "  "
            + sql_quote(content["notification_event_prefix"])
            + " || '-' || "
            + sql_quote(run_token)
            + " || '-' || ns.seq || '-' || SUBSTR(MD5("
            + sql_quote(run_token)
            + " || '-' || ns.seq::TEXT), 1, 8),"
        )
        a("  NOW()")
        a(f"FROM generate_series(0, {notification_count - 1}) AS ns(seq)")
        a("JOIN tmp_dummy_member m")
        a(f"  ON m.seq = ((ns.seq % {member_count}) + 1)")
        a("CROSS JOIN cfg")
        a("ON CONFLICT (event_id) WHERE event_id IS NOT NULL DO NOTHING;")
        a("")

    if favorite_count > 0:
        a("-- Member favorite restaurants")
        a("INSERT INTO member_favorite_restaurant (member_id, restaurant_id, created_at)")
        a("SELECT")
        a("  m.id,")
        a("  r.id,")
        a("  NOW()")
        a(f"FROM generate_series(0, {favorite_count - 1}) AS fs(seq)")
        a("JOIN tmp_dummy_member m")
        a(f"  ON m.seq = ((fs.seq % {member_count}) + 1)")
        a("JOIN tmp_dummy_restaurant r")
        a(f"  ON r.seq = (((fs.seq / {member_count}) % {restaurant_count}) + 1)")
        a("ON CONFLICT DO NOTHING;")
        a("")

    if subgroup_favorite_count > 0:
        a("-- Subgroup favorite restaurants")
        a("INSERT INTO subgroup_favorite_restaurant (member_id, subgroup_id, restaurant_id, created_at)")
        a("SELECT")
        a("  m.id,")
        a("  sg.id,")
        a("  r.id,")
        a("  NOW()")
        a(f"FROM generate_series(0, {subgroup_favorite_count - 1}) AS sfs(seq)")
        a("JOIN tmp_dummy_member m")
        a(f"  ON m.seq = ((sfs.seq % {member_count}) + 1)")
        a("JOIN tmp_dummy_subgroup sg")
        a(f"  ON sg.seq = (((sfs.seq / {member_count}) % {total_subgroups}) + 1)")
        subgroup_divisor = member_count * total_subgroups
        a("JOIN tmp_dummy_restaurant r")
        a(f"  ON r.seq = (((sfs.seq / {subgroup_divisor}) % {restaurant_count}) + 1)")
        a("ON CONFLICT DO NOTHING;")
        a("")

    a("-- Load-test bootstrap fixture for k6/locust scripts (phase1_test / locustfile)")
    a("CREATE TEMP TABLE tmp_loadtest_users (")
    a("  idx INTEGER PRIMARY KEY,")
    a("  identifier TEXT NOT NULL,")
    a("  nickname TEXT NOT NULL,")
    a("  member_id BIGINT")
    a(") ON COMMIT DROP;")
    a(
        "INSERT INTO tmp_loadtest_users (idx, identifier, nickname)"
    )
    a("SELECT")
    a("  gs,")
    a("  format('test-user-%s', lpad(gs::text, 3, '0')) AS identifier,")
    a("  format('부하테스트계정%s', gs) AS nickname")
    a("FROM generate_series(1, 1000) AS gs;")
    a("")
    a("-- Load-test members + OAuth account")
    a(
        "INSERT INTO member "
        "(id, email, nickname, status, role, profile_image_url, introduction, last_login_at, agreed_terms_at, agreed_privacy_at, created_at, updated_at)"
    )
    a("SELECT")
    a("  nextval('member_seq'),")
    a("  tu.identifier || '@test.local',")
    a("  tu.nickname,")
    a("  'ACTIVE',")
    a("  'USER',")
    a("  NULL,")
    a("  NULL,")
    a("  NOW(),")
    a("  NOW() - INTERVAL '1 day',")
    a("  NOW() - INTERVAL '1 day',")
    a("  NOW(),")
    a("  NOW()")
    a("FROM tmp_loadtest_users tu")
    a("ON CONFLICT (email) DO NOTHING;")
    a("")
    a("UPDATE tmp_loadtest_users tu")
    a("SET member_id = m.id")
    a("FROM member m")
    a("WHERE m.email = tu.identifier || '@test.local';")
    a("")
    a("INSERT INTO member_oauth_account (id, member_id, provider, provider_user_id, provider_user_email, created_at)")
    a("SELECT")
    a("  nextval('member_oauth_account_seq'),")
    a("  tu.member_id,")
    a("  'TEST',")
    a("  tu.identifier,")
    a("  tu.identifier || '@test.local',")
    a("  NOW()")
    a("FROM tmp_loadtest_users tu")
    a("WHERE tu.member_id IS NOT NULL")
    a("ON CONFLICT ON CONSTRAINT uk_member_oauth_provider_user DO NOTHING;")
    a("")
    a("INSERT INTO \"group\"")
    a(
        "(id, name, type, logo_image_url, address, detail_address, location, join_type, email_domain, status, created_at, updated_at)"
    )
    a("VALUES")
    a(
        "(2002, '부하테스트 그룹', 'UNOFFICIAL', NULL,"
        " '서울시 강남구 테스트로', '역삼동 2002',"
        " ST_SetSRID(ST_MakePoint(127.0276, 37.4979), 4326),"
        " 'PASSWORD', NULL, 'ACTIVE', NOW(), NOW())"
    )
    a("ON CONFLICT (id) DO UPDATE")
    a("SET")
    a("  name = EXCLUDED.name,")
    a("  type = EXCLUDED.type,")
    a("  logo_image_url = EXCLUDED.logo_image_url,")
    a("  address = EXCLUDED.address,")
    a("  detail_address = EXCLUDED.detail_address,")
    a("  location = EXCLUDED.location,")
    a("  join_type = EXCLUDED.join_type,")
    a("  email_domain = EXCLUDED.email_domain,")
    a("  status = EXCLUDED.status,")
    a("  updated_at = NOW();")
    a("")
    a(
        "INSERT INTO subgroup "
        "(id, group_id, name, description, profile_image_url, join_type, join_password, status, member_count, created_at, updated_at)"
    )
    a("VALUES")
    a(
        "(4002, 2002, '부하테스트 서브그룹', '부하테스트용 서브그룹입니다.', NULL,"
        " 'OPEN', NULL, 'ACTIVE', 0, NOW(), NOW())"
    )
    a("ON CONFLICT (id) DO UPDATE")
    a("SET")
    a("  group_id = EXCLUDED.group_id,")
    a("  name = EXCLUDED.name,")
    a("  description = EXCLUDED.description,")
    a("  profile_image_url = EXCLUDED.profile_image_url,")
    a("  join_type = EXCLUDED.join_type,")
    a("  join_password = EXCLUDED.join_password,")
    a("  status = EXCLUDED.status,")
    a("  updated_at = NOW();")
    a("")
    a("INSERT INTO chat_room (subgroup_id, created_at)")
    a("SELECT 4002, NOW()")
    a("WHERE NOT EXISTS (SELECT 1 FROM chat_room WHERE subgroup_id = 4002);")
    a("")
    a("CREATE TEMP TABLE tmp_loadtest_chat_room AS")
    a("SELECT id AS chat_room_id FROM chat_room WHERE subgroup_id = 4002 ORDER BY id LIMIT 1;")
    a("")
    a("INSERT INTO group_member (id, group_id, member_id, created_at)")
    a("SELECT nextval('group_member_seq'), 2002, tu.member_id, NOW()")
    a("FROM tmp_loadtest_users tu")
    a("WHERE tu.member_id IS NOT NULL")
    a("ON CONFLICT ON CONSTRAINT uk_group_member_group_id_member_id DO NOTHING;")
    a("")
    a("INSERT INTO subgroup_member (id, subgroup_id, member_id, created_at)")
    a("SELECT nextval('subgroup_member_seq'), 4002, tu.member_id, NOW()")
    a("FROM tmp_loadtest_users tu")
    a("WHERE tu.member_id IS NOT NULL")
    a("ON CONFLICT ON CONSTRAINT uk_subgroup_member_subgroup_id_member_id DO NOTHING;")
    a("")
    a("INSERT INTO chat_room_member (member_id, chat_room_id, created_at, updated_at)")
    a("SELECT")
    a("  tu.member_id,")
    a("  cr.chat_room_id,")
    a("  NOW(),")
    a("  NOW()")
    a("FROM tmp_loadtest_users tu")
    a("CROSS JOIN tmp_loadtest_chat_room cr")
    a("WHERE tu.member_id IS NOT NULL")
    a("ON CONFLICT (chat_room_id, member_id) DO NOTHING;")
    a("")
    a("UPDATE subgroup")
    a("SET member_count = (SELECT COUNT(*) FROM subgroup_member WHERE subgroup_id = 4002 AND deleted_at IS NULL)")
    a("WHERE id = 4002;")
    a("")
    a("DO $$")
    a("BEGIN")
    a("  IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pgcrypto') THEN")
    a("    RAISE EXCEPTION 'pgcrypto extension is required for group_auth_code bcrypt hash';")
    a("  END IF;")
    a("")
    a("  DELETE FROM group_auth_code WHERE group_id = 2002;")
    a(
        "  INSERT INTO group_auth_code (id, group_id, code, created_at)"
        " VALUES (nextval('group_auth_code_seq'), 2002, crypt('LOCAL-1234', gen_salt('bf', 12)), NOW());"
    )
    a("END $$;")
    a("")

    a("COMMIT;")
    a("")

    return "\n".join(lines)


def build_cleanup_sql(cfg: dict[str, Any]) -> str:
    content = cfg["content"]
    run_token = content["run_token"]

    member_like = content["member_email_prefix"].replace("%", "\\%") + "-" + run_token + "-%"
    group_like = content["group_name_prefix"].replace("%", "\\%") + "-" + run_token + "-%"
    restaurant_like = content["restaurant_name_prefix"].replace("%", "\\%") + "-" + run_token + "-%"
    event_like = content["notification_event_prefix"].replace("%", "\\%") + "-" + run_token + "-%"

    lines: list[str] = []
    a = lines.append

    a("-- Auto-generated cleanup SQL for Tasteam dummy data")
    a(f"-- run_token: {run_token}")
    a("")
    a("BEGIN;")
    a("SET LOCAL client_min_messages TO WARNING;")
    a("")

    a("CREATE TEMP TABLE tmp_cleanup_member_ids AS")
    a(f"SELECT id FROM member WHERE email LIKE {sql_quote(member_like)};")
    a("INSERT INTO tmp_cleanup_member_ids (id)")
    a(
        "SELECT moa.member_id "
        "FROM member_oauth_account moa "
        "WHERE moa.provider = 'TEST' "
        "  AND moa.provider_user_id ~ '^test-user-[0-9]{3}$';"
    )
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_group_ids AS")
    a(f"SELECT id FROM \"group\" WHERE name LIKE {sql_quote(group_like)};")
    a("INSERT INTO tmp_cleanup_group_ids (id)")
    a("SELECT 2002 WHERE EXISTS (SELECT 1 FROM \"group\" WHERE id = 2002);")
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_restaurant_ids AS")
    a(f"SELECT id FROM restaurant WHERE name LIKE {sql_quote(restaurant_like)};")
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_subgroup_ids AS")
    a("SELECT id FROM subgroup WHERE group_id IN (SELECT id FROM tmp_cleanup_group_ids);")
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_chat_room_ids AS")
    a("SELECT id FROM chat_room WHERE subgroup_id IN (SELECT id FROM tmp_cleanup_subgroup_ids);")
    a("")

    a("DELETE FROM member_notification_preference WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("DELETE FROM member_search_history WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("DELETE FROM member_favorite_restaurant WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a(
        "DELETE FROM subgroup_favorite_restaurant "
        "WHERE subgroup_id IN (SELECT id FROM tmp_cleanup_subgroup_ids) "
        "   OR member_id IN (SELECT id FROM tmp_cleanup_member_ids);"
    )
    a(f"DELETE FROM notification WHERE event_id LIKE {sql_quote(event_like)};")
    a("DELETE FROM notification WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("")
    a("DELETE FROM review_keyword WHERE review_id IN (")
    a("  SELECT id FROM review")
    a("  WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids)")
    a("     OR restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids)")
    a(");")
    a("DELETE FROM review WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids)")
    a("   OR restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);")
    a("")
    a("DELETE FROM chat_message WHERE chat_room_id IN (SELECT id FROM tmp_cleanup_chat_room_ids);")
    a("DELETE FROM chat_room_member WHERE chat_room_id IN (SELECT id FROM tmp_cleanup_chat_room_ids)")
    a("   OR member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("DELETE FROM chat_room WHERE id IN (SELECT id FROM tmp_cleanup_chat_room_ids);")
    a("")
    a("DELETE FROM subgroup_member WHERE subgroup_id IN (SELECT id FROM tmp_cleanup_subgroup_ids)")
    a("   OR member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("DELETE FROM member_oauth_account WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("DELETE FROM subgroup WHERE id IN (SELECT id FROM tmp_cleanup_subgroup_ids);")
    a("DELETE FROM group_member WHERE group_id IN (SELECT id FROM tmp_cleanup_group_ids)")
    a("   OR member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("DELETE FROM group_auth_code WHERE group_id IN (SELECT id FROM tmp_cleanup_group_ids);")
    a("DELETE FROM \"group\" WHERE id IN (SELECT id FROM tmp_cleanup_group_ids);")
    a("")
    a(
        "DELETE FROM restaurant_review_summary WHERE restaurant_id IN "
        "(SELECT id FROM tmp_cleanup_restaurant_ids);"
    )
    a(
        "DELETE FROM restaurant_review_sentiment WHERE restaurant_id IN "
        "(SELECT id FROM tmp_cleanup_restaurant_ids);"
    )
    a("DELETE FROM restaurant_food_category WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);")
    a("DELETE FROM restaurant_weekly_schedule WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);")
    a("DELETE FROM restaurant_address WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);")
    a("DELETE FROM menu WHERE category_id IN (")
    a("  SELECT id FROM menu_category WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids)")
    a(");")
    a("DELETE FROM menu_category WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);")
    a("DELETE FROM restaurant WHERE id IN (SELECT id FROM tmp_cleanup_restaurant_ids);")
    a("")
    a("DELETE FROM member WHERE id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("")
    a("COMMIT;")
    a("")

    return "\n".join(lines)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Tasteam 더미 데이터 INSERT SQL 생성기")
    parser.add_argument(
        "--config",
        type=Path,
        default=None,
        help="JSON 설정 파일 경로 (미지정 시 내장 기본값 사용)",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("scripts/dummy_sql_generator/generated_dummy_seed.sql"),
        help="생성할 seed SQL 파일 경로",
    )
    parser.add_argument(
        "--cleanup-output",
        type=Path,
        default=None,
        help="생성할 cleanup SQL 파일 경로 (선택)",
    )
    parser.add_argument(
        "--print-config",
        action="store_true",
        help="최종 머지된 설정(JSON)을 출력하고 종료",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    try:
        cfg = normalize_and_validate(read_config(args.config))
    except (OSError, json.JSONDecodeError, ConfigError) as exc:
        print(f"[ERROR] 설정 로딩/검증 실패: {exc}")
        return 1

    if args.print_config:
        print(json.dumps(cfg, ensure_ascii=False, indent=2))
        return 0

    seed_sql = build_seed_sql(cfg)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(seed_sql, encoding="utf-8")

    print(f"[OK] Seed SQL 생성 완료: {args.output}")
    print(f"[INFO] run_token={cfg['content']['run_token']}")

    if args.cleanup_output is not None:
        cleanup_sql = build_cleanup_sql(cfg)
        args.cleanup_output.parent.mkdir(parents=True, exist_ok=True)
        args.cleanup_output.write_text(cleanup_sql, encoding="utf-8")
        print(f"[OK] Cleanup SQL 생성 완료: {args.cleanup_output}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
