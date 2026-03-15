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

SCRIPT_DIR = Path(__file__).resolve().parent
# Spring BCryptPasswordEncoder().encode("1234") 로 생성한 고정 해시값
DEFAULT_GROUP_JOIN_CODE_BCRYPT_HASH = "$2a$10$PDQWS8fFPpIgAIwQY057NeKCd0WCUXAVX421zmYI97zawhlmVK3ki"

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
        "announcement_count": 12,
        "promotion_count": 8,
        "report_count": 2_500,
        "schedule_override_count": 2_000,
        "push_target_count": 3_000,
        "refresh_token_count": 1_000,
        "restaurant_image_count": 15_000,
        "review_image_count": 30_000,
        "member_image_count": 2_500,
        "group_image_count": 900,
        "subgroup_image_count": 2_000,
        "menu_domain_image_count": 12_000,
        "chat_image_count": 8_000,
        "image_optimization_job_count": 5_000,
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
        "restaurant_image_base_url": "https://cdn.tasteam.kr/restaurants",
        "review_image_base_url": "https://cdn.tasteam.kr/reviews",
        "chat_image_base_url": "https://cdn.tasteam.kr/chats",
        "promotion_image_base_url": "https://cdn.tasteam.kr/promotions",
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
        "sidos": ["서울시", "서울시", "서울시", "서울시", "서울시", "서울시", "서울시", "경기도", "경기도", "경기도", "부산시", "인천시", "대구시"],
        "sigungus": ["강남구", "서초구", "마포구", "송파구", "강서구", "종로구", "중구", "분당구", "수원시", "일산동구", "해운대구", "연수구", "수성구"],
        "eupmyeondong": "역삼동",
        "postal_code": "06000",
        "summary_text": "더미 리뷰 요약입니다.",
        "summary_highlights": ["맛있다", "친절하다"],
        "notification_event_prefix": "dummy",
        "realistic_names": False,
        "seoul_focused_coords": False,
    },
    "tuning": {
        "menu_per_category": 3,
        "base_menu_price": 8_000,
        "max_keywords_per_review": 3,
        "include_async_log_data": False,
        "async_log_scale_percent": 30,
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
    "announcement_count": 500,
    "promotion_count": 500,
    "report_count": 200_000,
    "schedule_override_count": 200_000,
    "push_target_count": 100_000,
    "refresh_token_count": 100_000,
    "restaurant_image_count": 300_000,
    "review_image_count": 1_000_000,
    "member_image_count": 100_000,
    "group_image_count": 20_000,
    "subgroup_image_count": 100_000,
    "menu_domain_image_count": 1_000_000,
    "chat_image_count": 500_000,
    "image_optimization_job_count": 500_000,
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
    tuning["include_async_log_data"] = bool(tuning.get("include_async_log_data", False))
    tuning["async_log_scale_percent"] = require_int(
        "async_log_scale_percent", tuning.get("async_log_scale_percent"), 0
    )
    if tuning["async_log_scale_percent"] > 300:
        raise ConfigError("async_log_scale_percent 값은 300 이하여야 합니다.")

    for key in [
        "member_email_prefix",
        "member_email_suffix",
        "member_nickname_prefix",
        "profile_image_base_url",
        "group_logo_base_url",
        "subgroup_profile_base_url",
        "menu_image_base_url",
        "restaurant_image_base_url",
        "review_image_base_url",
        "chat_image_base_url",
        "promotion_image_base_url",
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

    content["realistic_names"] = bool(content.get("realistic_names", False))
    content["seoul_focused_coords"] = bool(content.get("seoul_focused_coords", False))

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
    announcement_count = counts["announcement_count"]
    promotion_count = counts["promotion_count"]
    report_count = counts["report_count"]
    schedule_override_count = counts["schedule_override_count"]
    push_target_count = counts["push_target_count"]
    refresh_token_count = counts["refresh_token_count"]
    restaurant_image_count = counts["restaurant_image_count"]
    review_image_count = counts["review_image_count"]
    member_image_count = counts["member_image_count"]
    group_image_count = counts["group_image_count"]
    subgroup_image_count = counts["subgroup_image_count"]
    menu_domain_image_count = counts["menu_domain_image_count"]
    chat_image_count = counts["chat_image_count"]
    image_optimization_job_count = counts["image_optimization_job_count"]

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


def sql_md5_expr(seed_expr: str) -> str:
    return f"MD5({seed_expr})"


def sql_hash_slice_expr(seed_expr: str, start: int, length: int) -> str:
    return f"SUBSTR({sql_md5_expr(seed_expr)}, {start}, {length})"


def sql_bucket_expr(seed_expr: str, offset: int, modulo: int, base: int = 0) -> str:
    return f"(MOD(ASCII(SUBSTR({sql_md5_expr(seed_expr)}, {offset}, 1)), {modulo}) + {base})"


def sql_variant_case_expr(seed_expr: str, variants: list[str], offset: int = 2) -> str:
    if not variants:
        raise ValueError("variants는 최소 1개 이상이어야 합니다.")
    if len(variants) == 1:
        return sql_quote(variants[0])

    bucket_expr = sql_bucket_expr(seed_expr, offset, len(variants))
    parts = [f"CASE {bucket_expr}"]
    for idx, variant in enumerate(variants):
        parts.append(f" WHEN {idx} THEN {sql_quote(variant)}")
    parts.append(f" ELSE {sql_quote(variants[-1])} END")
    return "".join(parts)


def sql_asset_url_expr(
    base_url: str,
    run_token: str,
    entity_path_expr: str,
    ext: str,
    seed_expr: str,
    variants: list[str],
) -> str:
    shard_expr = sql_bucket_expr(seed_expr, 1, 16, 1)
    return (
        sql_quote(base_url)
        + " || '/' || "
        + sql_quote(run_token)
        + " || '/edge-' || LPAD(("
        + shard_expr
        + ")::TEXT, 2, '0') || '/' || "
        + entity_path_expr
        + " || '/' || "
        + sql_hash_slice_expr(seed_expr, 1, 10)
        + " || '-' || "
        + sql_hash_slice_expr(seed_expr, 11, 8)
        + " || '."
        + ext
        + "?tr=' || "
        + sql_variant_case_expr(seed_expr, variants, 2)
    )


def sql_storage_key_expr(
    prefix: str,
    run_token: str,
    entity_path_expr: str,
    ext: str,
    seed_expr: str,
) -> str:
    shard_expr = sql_bucket_expr(seed_expr, 3, 16, 1)
    return (
        sql_quote(prefix)
        + " || '/' || "
        + sql_quote(run_token)
        + " || '/bucket-' || LPAD(("
        + shard_expr
        + ")::TEXT, 2, '0') || '/' || "
        + entity_path_expr
        + " || '/' || "
        + sql_hash_slice_expr(seed_expr, 1, 12)
        + " || '-' || "
        + sql_hash_slice_expr(seed_expr, 13, 8)
        + " || '."
        + ext
        + "'"
    )


def sql_uuid_expr(seed_expr: str) -> str:
    return (
        "("
        f"SUBSTR(MD5({seed_expr}), 1, 8) || '-' || "
        f"SUBSTR(MD5({seed_expr}), 9, 4) || '-' || "
        f"SUBSTR(MD5({seed_expr}), 13, 4) || '-' || "
        f"SUBSTR(MD5({seed_expr}), 17, 4) || '-' || "
        f"SUBSTR(MD5({seed_expr}), 21, 12)"
        ")::uuid"
    )


def sql_hex64_expr(seed_expr: str) -> str:
    return f"(MD5({seed_expr}) || MD5({seed_expr} || '-seed'))"


def estimate_member_search_history_rows(member_count: int) -> int:
    full_cycles = member_count // 4
    remainder = member_count % 4
    remainder_totals = [0, 2, 5, 9]
    return (full_cycles * 14) + remainder_totals[remainder]


def scale_count(base_count: int, scale_percent: int) -> int:
    if base_count <= 0 or scale_percent <= 0:
        return 0
    return max(1, (base_count * scale_percent + 99) // 100)


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
    announcement_count = counts["announcement_count"]
    promotion_count = counts["promotion_count"]
    report_count = counts["report_count"]
    schedule_override_count = counts["schedule_override_count"]
    push_target_count = counts["push_target_count"]
    refresh_token_count = counts["refresh_token_count"]
    restaurant_image_count = counts["restaurant_image_count"]
    review_image_count = counts["review_image_count"]
    member_image_count = counts["member_image_count"]
    group_image_count = counts["group_image_count"]
    subgroup_image_count = counts["subgroup_image_count"]
    menu_domain_image_count = counts["menu_domain_image_count"]
    chat_image_count = counts["chat_image_count"]
    image_optimization_job_count = counts["image_optimization_job_count"]

    run_token = content["run_token"]
    group_join_code_hash_sql = sql_quote(DEFAULT_GROUP_JOIN_CODE_BCRYPT_HASH)
    menu_per_category = tuning["menu_per_category"]
    base_menu_price = tuning["base_menu_price"]
    max_keywords_per_review = tuning["max_keywords_per_review"]
    include_async_log_data = tuning["include_async_log_data"]
    async_log_scale_percent = tuning["async_log_scale_percent"]
    realistic_names = content.get("realistic_names", False)
    seoul_coords = content.get("seoul_focused_coords", False)

    total_subgroups = group_count * subgroup_per_group
    members_per_group_effective = min(member_count, member_per_group)
    rows_per_member_pref = len(content["notification_channels"]) * len(content["notification_types"])
    safe_total_subgroups = max(total_subgroups, 1)
    safe_group_count = max(group_count, 1)
    restaurant_hotspot_size = max(1, restaurant_count // 10) if restaurant_count > 0 else 0
    restaurant_tail_size = restaurant_count - restaurant_hotspot_size
    # 파레토 분포: 상위 20% 멤버가 리뷰의 80% 차지 (활성 유저 vs 휴면 유저 시나리오)
    active_member_count = max(1, member_count // 5)
    inactive_member_count = max(1, member_count - active_member_count)
    total_menu_count = restaurant_count * len(content["menu_category_names"]) * menu_per_category

    effective_announcement_count = announcement_count
    effective_promotion_count = promotion_count
    effective_report_count = min(report_count, member_count * 5) if member_count > 0 else 0
    effective_schedule_override_count = min(schedule_override_count, restaurant_count)
    effective_push_target_count = min(push_target_count, member_count)
    effective_refresh_token_count = min(refresh_token_count, member_count)
    effective_restaurant_image_count = min(restaurant_image_count, restaurant_count * 3)
    effective_review_image_count = min(review_image_count, review_count)
    effective_member_image_count = min(member_image_count, member_count)
    effective_group_image_count = min(group_image_count, group_count)
    effective_subgroup_image_count = min(subgroup_image_count, total_subgroups)
    effective_menu_domain_image_count = min(menu_domain_image_count, total_menu_count)
    effective_chat_image_count = min(chat_image_count, total_subgroups * chat_message_per_room)

    total_image_seed_count = (
        effective_restaurant_image_count
        + effective_review_image_count
        + effective_member_image_count
        + effective_group_image_count
        + effective_subgroup_image_count
        + effective_menu_domain_image_count
        + effective_chat_image_count
        + (effective_promotion_count * 3)
    )
    effective_image_optimization_job_count = min(image_optimization_job_count, total_image_seed_count)
    estimated_member_search_history_count = estimate_member_search_history_rows(member_count)
    seeded_group_member_count = group_count * members_per_group_effective
    base_user_activity_group_join_count = seeded_group_member_count
    base_user_activity_review_created_count = review_count
    base_user_activity_search_count = estimated_member_search_history_count
    base_user_activity_favorite_count = favorite_count
    base_user_activity_restaurant_view_count = (
        min((review_count // 2) + favorite_count, member_count * 12) if member_count > 0 else 0
    )
    base_user_activity_page_view_count = member_count * 6
    base_user_activity_page_dwelled_count = member_count * 4
    effective_user_activity_group_join_count = (
        scale_count(base_user_activity_group_join_count, async_log_scale_percent)
        if include_async_log_data
        else 0
    )
    effective_user_activity_review_created_count = (
        scale_count(base_user_activity_review_created_count, async_log_scale_percent)
        if include_async_log_data
        else 0
    )
    effective_user_activity_search_count = (
        scale_count(base_user_activity_search_count, async_log_scale_percent)
        if include_async_log_data
        else 0
    )
    effective_user_activity_favorite_count = (
        scale_count(base_user_activity_favorite_count, async_log_scale_percent)
        if include_async_log_data
        else 0
    )
    effective_user_activity_restaurant_view_count = (
        scale_count(base_user_activity_restaurant_view_count, async_log_scale_percent)
        if include_async_log_data
        else 0
    )
    effective_user_activity_page_view_count = (
        scale_count(base_user_activity_page_view_count, async_log_scale_percent)
        if include_async_log_data
        else 0
    )
    effective_user_activity_page_dwelled_count = (
        scale_count(base_user_activity_page_dwelled_count, async_log_scale_percent)
        if include_async_log_data
        else 0
    )
    total_user_activity_event_count = (
        effective_user_activity_group_join_count
        + effective_user_activity_review_created_count
        + effective_user_activity_search_count
        + effective_user_activity_favorite_count
        + effective_user_activity_restaurant_view_count
        + effective_user_activity_page_view_count
        + effective_user_activity_page_dwelled_count
    )
    effective_notification_outbox_count = (
        min(notification_count, scale_count(notification_count, async_log_scale_percent))
        if include_async_log_data
        else 0
    )
    async_source_failed_percent = 3
    async_source_pending_percent = 7
    async_dispatch_failed_percent = 2
    async_dispatch_pending_percent = 5
    async_notification_failed_percent = 4
    async_notification_pending_percent = 8

    summary_json = jsonb_literal(
        {
            "overall_summary": content["summary_text"],
            "categories": {},
            "highlights": content["summary_highlights"],
        }
    )

    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S %Z")

    lines: list[str] = []
    a = lines.append

    a("-- Auto-generated dummy seed SQL for Tasteam")
    a(f"-- generated_at: {generated_at}")
    a(f"-- run_token: {run_token}")
    a("-- This script is generated by loadtest/seed/generate_dummy_seed_sql.py")
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

    a("-- Reference dictionaries bootstrap for fresh DB")
    a("INSERT INTO food_category (name)")
    a("SELECT fc.name")
    a(
        "FROM UNNEST("
        + sql_text_array(["한식", "중식", "일식", "양식", "카페", "치킨", "피자"])
        + "::TEXT[]) AS fc(name)"
    )
    a("WHERE NOT EXISTS (SELECT 1 FROM food_category);")
    a("")
    a("WITH seed_keyword(type, name) AS (")
    a("  VALUES")
    a("    ('VISIT_PURPOSE', '점심'),")
    a("    ('VISIT_PURPOSE', '저녁'),")
    a("    ('VISIT_PURPOSE', '회식'),")
    a("    ('VISIT_PURPOSE', '데이트'),")
    a("    ('COMPANION_TYPE', '혼밥'),")
    a("    ('COMPANION_TYPE', '친구'),")
    a("    ('COMPANION_TYPE', '연인'),")
    a("    ('COMPANION_TYPE', '동료'),")
    a("    ('WAITING_EXPERIENCE', '바로 입장'),")
    a("    ('WAITING_EXPERIENCE', '대기 짧음'),")
    a("    ('WAITING_EXPERIENCE', '대기 있음'),")
    a("    ('POSITIVE_ASPECT', '맛있음'),")
    a("    ('POSITIVE_ASPECT', '친절함'),")
    a("    ('POSITIVE_ASPECT', '가성비 좋음'),")
    a("    ('POSITIVE_ASPECT', '분위기 좋음')")
    a(")")
    a("INSERT INTO keyword (type, name)")
    a("SELECT sk.type, sk.name")
    a("FROM seed_keyword sk")
    a("WHERE NOT EXISTS (SELECT 1 FROM keyword);")
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
            + sql_asset_url_expr(
                content["profile_image_base_url"],
                run_token,
                "'member/profile/' || m.id",
                "webp",
                sql_quote(run_token) + " || '-member-profile-' || m.id::TEXT",
                [
                    "w=160&q=72&fit=cover",
                    "w=320&q=78&fit=cover",
                    "w=640&q=82&fit=cover",
                ],
            )
            + " END,"
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
        a("  NOW() - ((m.seq % 365) || ' days')::INTERVAL,")
        a("  NOW() - ((m.seq % 365) || ' days')::INTERVAL")
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
        if realistic_names:
            a(
                "  cfg.left_tokens[((r.seq - 1) % CARDINALITY(cfg.left_tokens)) + 1] || ' ' || "
                "cfg.right_tokens[((r.seq - 1) % CARDINALITY(cfg.right_tokens)) + 1] || ' ' || "
                "(MOD(r.seq, 9) + 1) || '호점',"
            )
        else:
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
        if seoul_coords:
            a(
                "  ST_SetSRID(ST_MakePoint(126.85 + MOD((r.seq * 37)::BIGINT, 300) / 1000.0, "
                "37.45 + MOD((r.seq * 53)::BIGINT, 200) / 1000.0), 4326),"
            )
        else:
            a(
                "  ST_SetSRID(ST_MakePoint(126.0 + MOD((r.seq * 37)::BIGINT, 3600) / 1000.0, "
                "33.1 + MOD((r.seq * 53)::BIGINT, 5500) / 1000.0), 4326),"
            )
        a("  '0' || (2 + MOD(r.seq, 8)) || '-' || LPAD((1000 + MOD(r.seq * 31, 9000))::TEXT, 4, '0') || '-' || LPAD((1000 + MOD(r.seq * 17, 9000))::TEXT, 4, '0'),")
        a("  0,")
        a("  NOW() - ((r.seq % 365) || ' days')::INTERVAL,")
        a("  NOW() - ((r.seq % 365) || ' days')::INTERVAL")
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
            + sql_asset_url_expr(
                content["menu_image_base_url"],
                run_token,
                "'menu/' || mc.restaurant_id || '/' || (((mc.seq - 1) * "
                + str(menu_per_category)
                + ") + m.menu_idx)",
                "jpg",
                sql_quote(run_token)
                + " || '-menu-card-' || mc.restaurant_id::TEXT || '-' || (((mc.seq - 1) * "
                + str(menu_per_category)
                + ") + m.menu_idx)::TEXT",
                [
                    "w=360&q=74&fit=cover",
                    "w=720&q=80&fit=cover",
                    "w=1080&q=84&fit=cover",
                ],
            )
            + " END,"
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
        if realistic_names:
            a(
                "  cfg.topics[((g.seq - 1) % CARDINALITY(cfg.topics)) + 1] || ' ' || g.seq || '팀',"
            )
        else:
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
            + sql_asset_url_expr(
                content["group_logo_base_url"],
                run_token,
                "'group/logo/' || g.id",
                "png",
                sql_quote(run_token) + " || '-group-logo-' || g.id::TEXT",
                [
                    "w=256&q=88&fit=contain",
                    "w=512&q=90&fit=contain",
                    "w=1024&q=92&fit=contain",
                ],
            )
            + " END,"
        )
        a(
            "  cfg.sidos[((g.seq - 1) % CARDINALITY(cfg.sidos)) + 1] || ' ' || "
            "cfg.sigungus[((g.seq - 1) % CARDINALITY(cfg.sigungus)) + 1] || ' ' || "
            + sql_quote(content["group_address_prefix"])
            + " || ' ' || g.seq,"
        )
        a("  '빌딩 ' || (MOD(g.seq, 40) + 1) || '층',")
        if seoul_coords:
            a(
                "  ST_SetSRID(ST_MakePoint(126.85 + MOD((g.seq * 41)::BIGINT, 300) / 1000.0, "
                "37.45 + MOD((g.seq * 59)::BIGINT, 200) / 1000.0), 4326),"
            )
        else:
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
        a("  NOW() - ((g.seq % 365) || ' days')::INTERVAL,")
        a("  NOW() - ((g.seq % 365) || ' days')::INTERVAL")
        a("FROM tmp_dummy_group g")
        a("CROSS JOIN cfg")
        a("ORDER BY g.seq;")
        a("")

        a("-- Group auth codes for PASSWORD groups")
        a("INSERT INTO group_auth_code (id, group_id, code, created_at)")
        a("SELECT")
        a("  nextval('group_auth_code_seq'),")
        a("  g.id,")
        a(f"  {group_join_code_hash_sql},")
        a("  NOW()")
        a("FROM tmp_dummy_group tg")
        a("JOIN \"group\" g ON g.id = tg.id")
        a("WHERE g.join_type = 'PASSWORD'")
        a("ORDER BY tg.seq;")
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
            + sql_asset_url_expr(
                content["subgroup_profile_base_url"],
                run_token,
                "'subgroup/profile/' || sg.id",
                "jpg",
                sql_quote(run_token) + " || '-subgroup-profile-' || sg.id::TEXT",
                [
                    "w=240&q=76&fit=cover",
                    "w=480&q=82&fit=cover",
                    "w=960&q=86&fit=cover",
                ],
            )
            + " END,"
        )
        a("  CASE WHEN MOD(sg.seq, 5) = 0 THEN 'PASSWORD' ELSE 'OPEN' END,")
        a(
            "  CASE WHEN MOD(sg.seq, 5) = 0 THEN "
            + sql_quote(content["subgroup_password_prefix"])
            + " || '-' || sg.group_seq || '-' || sg.subgroup_index ELSE NULL END,"
        )
        a("  'ACTIVE',")
        a("  0,")
        a("  NOW() - ((sg.seq % 300) || ' days')::INTERVAL,")
        a("  NOW() - ((sg.seq % 300) || ' days')::INTERVAL")
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
        a("-- 명시적 id 삽입으로 인해 identity 시퀀스가 뒤처지는 문제 방지")
        a("SELECT setval(pg_get_serial_sequence('chat_room', 'id'), COALESCE((SELECT MAX(id) FROM chat_room), 0), true);")
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
        a("  NOW() - ((cr.seq % 90) || ' days')::INTERVAL")
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
        a("  (MOD(rv.seq - 1, 4) != 0)")
        a(f"FROM generate_series(1, {review_count}) AS rv(seq)")
        a("JOIN tmp_dummy_member m")
        a(
            # 파레토: 80% 리뷰 → 상위 20% 멤버(활성 유저), 20% 리뷰 → 하위 80% 멤버(휴면 유저)
            f"  ON m.seq = (CASE WHEN MOD(rv.seq - 1, 10) < 8 "
            f"THEN ((rv.seq - 1) % {active_member_count}) + 1 "
            f"ELSE {active_member_count} + (((rv.seq - 1) % {inactive_member_count}) + 1) END)"
        )
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
        a("  NOW() - ((rv.seq % 180) || ' days')::INTERVAL,")
        a("  NOW() - ((rv.seq % 180) || ' days')::INTERVAL")
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
        a("ON CONFLICT (restaurant_id) DO NOTHING;")
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
            "  END || '?nav=' || "
            + sql_variant_case_expr(
                sql_quote(run_token) + " || '-notification-link-' || ns.seq::TEXT",
                [
                    "push&surface=inbox",
                    "digest&surface=badge",
                    "event&surface=banner",
                    "recommend&surface=feed",
                ],
            )
            + ","
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
        a("  NOW() - ((ns.seq % 90) || ' days')::INTERVAL")
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
        a("  NOW() - ((fs.seq % 180) || ' days')::INTERVAL")
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
        a("  NOW() - ((sfs.seq % 180) || ' days')::INTERVAL")
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

    if effective_announcement_count > 0:
        a("-- Announcements")
        a("WITH cfg AS (")
        a(
            f"  SELECT {sql_text_array(['서비스 점검 안내', '추천 피드 개편 안내', '검색 품질 개선 안내', '리뷰 정책 업데이트', '그룹 기능 개선 안내', '알림 설정 개편 안내'])}::TEXT[] AS titles,"
        )
        a(
            f"         {sql_text_array(['새벽 시간대 점검으로 일부 기능이 일시적으로 제한될 수 있습니다.', '점심·회식·카페 상황에 맞춘 추천 이유가 더 또렷하게 노출됩니다.', '지역·카테고리·분위기 필터 조합 검색 응답 속도를 개선했습니다.', '허위·중복 리뷰 신고 기준을 정리해 운영 응답 속도를 높였습니다.', '그룹과 서브그룹 멤버 구성 정보가 더 명확하게 표시됩니다.', '중요 공지와 채팅 알림을 분리해 설정할 수 있도록 구조를 다듬었습니다.'])}::TEXT[] AS bodies"
        )
        a(")")
        a("INSERT INTO announcement (title, content, deleted_at, created_at, updated_at)")
        a("SELECT")
        a(
            "  '시드공지-' || "
            + sql_quote(run_token)
            + " || '-' || LPAD((ann.seq + 1)::TEXT, 3, '0') || ' · ' || cfg.titles[(ann.seq % CARDINALITY(cfg.titles)) + 1],"
        )
        a(
            "  cfg.bodies[(ann.seq % CARDINALITY(cfg.bodies)) + 1] || "
            "' 운영 공지 상세 본문은 관리자 화면과 사용자 공지 목록에서 함께 확인할 수 있습니다.',"
        )
        a("  NULL,")
        a("  NOW() - ((ann.seq % 60) || ' days')::INTERVAL,")
        a("  NOW() - ((ann.seq % 60) || ' days')::INTERVAL + INTERVAL '10 minutes'")
        a(f"FROM generate_series(0, {effective_announcement_count - 1}) AS ann(seq)")
        a("CROSS JOIN cfg;")
        a("")

    if effective_promotion_count > 0:
        a("-- Promotions")
        a("CREATE TEMP TABLE tmp_dummy_promotion (seq INTEGER PRIMARY KEY, id BIGINT NOT NULL) ON COMMIT DROP;")
        a("INSERT INTO tmp_dummy_promotion (seq, id)")
        a("SELECT gs, nextval(pg_get_serial_sequence('promotion', 'id'))")
        a(f"FROM generate_series(1, {effective_promotion_count}) AS gs;")
        a("")
        a("WITH cfg AS (")
        a(
            f"  SELECT {sql_text_array(['강남 점심 할인전', '퇴근 후 회식 지원전', '주말 브런치 큐레이션', '비 오는 날 국물 추천전', '야식 인기 메뉴전', '신규 그룹 환영 쿠폰'])}::TEXT[] AS titles,"
        )
        a(
            f"         {sql_text_array(['점심 시간 직장인 수요가 높은 지역 맛집을 중심으로 즉시 사용 가능한 쿠폰을 제공합니다.', '팀 회식·소규모 모임에 어울리는 가게를 묶어 추천하고 프로모션 혜택을 함께 노출합니다.', '카페와 브런치 매장을 중심으로 주말 탐색형 추천을 제공합니다.', '국밥·라멘·짬뽕처럼 날씨 맥락이 강한 메뉴를 중심으로 추천합니다.', '늦은 시간대 배달·포장 선호가 높은 메뉴를 중심으로 구성했습니다.', '첫 그룹 생성과 첫 리뷰 작성 흐름을 자연스럽게 유도하는 프로모션입니다.'])}::TEXT[] AS contents,"
        )
        a(f"         {sql_text_array(['BOTH', 'PROMOTION_LIST', 'MAIN_BANNER'])}::TEXT[] AS channels")
        a(")")
        a(
            "INSERT INTO promotion "
            "(id, title, content, landing_url, promotion_start_at, promotion_end_at, publish_status, deleted_at, created_at, updated_at)"
        )
        a("SELECT")
        a("  p.id,")
        a(
            "  '시드프로모션-' || "
            + sql_quote(run_token)
            + " || '-' || LPAD(p.seq::TEXT, 3, '0') || ' · ' || cfg.titles[((p.seq - 1) % CARDINALITY(cfg.titles)) + 1],"
        )
        a("  cfg.contents[((p.seq - 1) % CARDINALITY(cfg.contents)) + 1],")
        a(
            "  'https://tasteam.kr/promotions/' || "
            + sql_quote(run_token)
            + " || '/' || p.id || '/' || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-promotion-landing-' || p.id::TEXT",
                1,
                8,
            )
            + " || '?utm=' || "
            + sql_variant_case_expr(
                sql_quote(run_token) + " || '-promotion-query-' || p.id::TEXT",
                [
                    "app-banner&utm_medium=hero&utm_campaign=lunch-promo",
                    "app-feed&utm_medium=card&utm_campaign=team-dining",
                    "push&utm_medium=deeplink&utm_campaign=weekend-brunch",
                    "search&utm_medium=reco&utm_campaign=night-snack",
                ],
            )
            + ","
        )
        a("  NOW() - ((p.seq % 14) || ' days')::INTERVAL,")
        a("  NOW() + ((20 + (p.seq % 40)) || ' days')::INTERVAL,")
        a("  CASE WHEN MOD(p.seq, 8) = 0 THEN 'ARCHIVED' WHEN MOD(p.seq, 5) = 0 THEN 'DRAFT' ELSE 'PUBLISHED' END,")
        a("  NULL,")
        a("  NOW() - ((p.seq % 30) || ' days')::INTERVAL,")
        a("  NOW() - ((p.seq % 30) || ' days')::INTERVAL + INTERVAL '15 minutes'")
        a("FROM tmp_dummy_promotion p")
        a("CROSS JOIN cfg")
        a("ORDER BY p.seq;")
        a("")
        a("WITH cfg AS (")
        a(f"  SELECT {sql_text_array(['BOTH', 'PROMOTION_LIST', 'MAIN_BANNER'])}::TEXT[] AS channels")
        a(")")
        a(
            "INSERT INTO promotion_display "
            "(promotion_id, display_enabled, display_start_at, display_end_at, display_channel, display_priority, deleted_at, created_at, updated_at)"
        )
        a("SELECT")
        a("  p.id,")
        a("  (MOD(p.seq, 6) <> 0),")
        a("  NOW() - ((p.seq % 10) || ' days')::INTERVAL,")
        a("  NOW() + ((15 + (p.seq % 35)) || ' days')::INTERVAL,")
        a("  cfg.channels[((p.seq - 1) % CARDINALITY(cfg.channels)) + 1],")
        a("  p.seq,")
        a("  NULL,")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_promotion p")
        a("CROSS JOIN cfg")
        a("ON CONFLICT (promotion_id) DO NOTHING;")
        a("")
        a(
            "CREATE TEMP TABLE tmp_dummy_promotion_asset "
            "(seq INTEGER PRIMARY KEY, promotion_id BIGINT NOT NULL, asset_type TEXT NOT NULL, sort_order INTEGER NOT NULL, is_primary BOOLEAN NOT NULL, image_url TEXT NOT NULL, alt_text TEXT NOT NULL) ON COMMIT DROP;"
        )
        a("INSERT INTO tmp_dummy_promotion_asset (seq, promotion_id, asset_type, sort_order, is_primary, image_url, alt_text)")
        a("SELECT")
        a("  ROW_NUMBER() OVER (ORDER BY p.seq, asset.sort_order),")
        a("  p.id,")
        a("  asset.asset_type,")
        a("  asset.sort_order,")
        a("  asset.is_primary,")
        a(
            "  "
            + sql_asset_url_expr(
                content["promotion_image_base_url"],
                run_token,
                "'promotion/' || p.id || '/' || LOWER(asset.asset_type)",
                "webp",
                sql_quote(run_token)
                + " || '-promotion-asset-' || p.id::TEXT || '-' || asset.asset_type",
                [
                    "w=720&q=76&fit=cover",
                    "w=1280&q=82&fit=cover",
                    "w=1440&q=86&fit=cover",
                ],
            )
            + ","
        )
        a(
            "  CASE asset.asset_type "
            "    WHEN 'BANNER' THEN '프로모션 메인 배너' "
            "    WHEN 'SPLASH' THEN '스플래시 노출 이미지' "
            "    ELSE '프로모션 상세 이미지' "
            "  END"
        )
        a("FROM tmp_dummy_promotion p")
        a("CROSS JOIN (VALUES ('BANNER', 0, true), ('SPLASH', 1, false), ('DETAIL', 2, false)) AS asset(asset_type, sort_order, is_primary);")
        a("")
        a(
            "INSERT INTO promotion_asset "
            "(promotion_id, asset_type, image_url, alt_text, sort_order, is_primary, deleted_at, created_at, updated_at)"
        )
        a("SELECT")
        a("  promotion_id,")
        a("  asset_type,")
        a("  image_url,")
        a("  alt_text,")
        a("  sort_order,")
        a("  is_primary,")
        a("  NULL,")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_promotion_asset")
        a("ORDER BY seq;")
        a("")

    if effective_push_target_count > 0:
        a("-- Push notification targets")
        a("INSERT INTO push_notification_target (member_id, device_id, fcm_token, created_at)")
        a("SELECT")
        a("  seeded.member_id,")
        a(
            "  'device-' || "
            + sql_quote(run_token)
            + " || '-slot-' || LPAD(("
            + sql_bucket_expr(
                sql_quote(run_token) + " || '-push-device-' || seeded.member_id::TEXT",
                1,
                24,
                1,
            )
            + ")::TEXT, 2, '0') || '-' || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-push-device-' || seeded.member_id::TEXT",
                1,
                16,
            )
            + ","
        )
        a(
            "  'fcm-' || "
            + sql_quote(run_token)
            + " || '-' || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-push-fcm-' || seeded.member_id::TEXT",
                1,
                24,
            )
            + " || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-push-fcm-tail-' || seeded.member_id::TEXT",
                1,
                24,
            )
            + ","
        )
        a("  NOW() - ((seeded.rn % 30) || ' days')::INTERVAL")
        a("FROM (")
        a("  SELECT DISTINCT m.id AS member_id, ROW_NUMBER() OVER (ORDER BY m.seq) AS rn")
        a("  FROM tmp_dummy_member m")
        a("  JOIN member_notification_preference pref")
        a("    ON pref.member_id = m.id")
        a("   AND pref.channel = 'PUSH'")
        a("   AND pref.is_enabled = true")
        a(") seeded")
        a(f"WHERE seeded.rn <= {effective_push_target_count}")
        a("ON CONFLICT (member_id, device_id) DO NOTHING;")
        a("")

    if effective_refresh_token_count > 0:
        a("-- Refresh tokens")
        a(
            "INSERT INTO refresh_token "
            "(id, member_id, token_hash, token_family_id, expires_at, rotated_at, revoked_at, created_at)"
        )
        a("SELECT")
        a("  nextval('refresh_token_seq'),")
        a("  m.id,")
        a("  " + sql_hex64_expr(sql_quote(run_token) + " || '-refresh-' || m.seq::TEXT") + ",")
        a("  " + sql_hex64_expr(sql_quote(run_token) + " || '-family-' || m.seq::TEXT") + ",")
        a("  NOW() + ((20 + (m.seq % 40)) || ' days')::INTERVAL,")
        a("  CASE WHEN MOD(m.seq, 9) = 0 THEN NOW() - ((m.seq % 5) || ' days')::INTERVAL ELSE NULL END,")
        a("  CASE WHEN MOD(m.seq, 17) = 0 THEN NOW() - ((m.seq % 3) || ' days')::INTERVAL ELSE NULL END,")
        a("  NOW() - ((m.seq % 25) || ' days')::INTERVAL")
        a("FROM tmp_dummy_member m")
        a(f"WHERE m.seq <= {effective_refresh_token_count}")
        a("ON CONFLICT (token_hash) DO NOTHING;")
        a("")

    if effective_schedule_override_count > 0:
        a("-- Restaurant schedule overrides")
        a(
            "INSERT INTO restaurant_schedule_override "
            "(restaurant_id, date, open_time, close_time, is_closed, reason, created_at, updated_at)"
        )
        a("SELECT")
        a("  r.id,")
        a("  CURRENT_DATE + ((r.seq % 21) + 1),")
        a("  CASE WHEN MOD(r.seq, 4) = 0 THEN NULL ELSE TIME '10:30:00' END,")
        a("  CASE WHEN MOD(r.seq, 4) = 0 THEN NULL ELSE TIME '15:30:00' END,")
        a("  (MOD(r.seq, 4) = 0),")
        a(
            "  '시드예외-"
            + run_token
            + "-' || CASE MOD(r.seq, 5) "
            "    WHEN 0 THEN '임시 휴무' "
            "    WHEN 1 THEN '브레이크타임 조정' "
            "    WHEN 2 THEN '단체 예약 운영' "
            "    WHEN 3 THEN '재료 소진 대응' "
            "    ELSE '공휴일 운영 시간 조정' "
            "  END,"
        )
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_dummy_restaurant r")
        a(f"WHERE r.seq <= {effective_schedule_override_count}")
        a("ON CONFLICT (restaurant_id, date) DO NOTHING;")
        a("")

    if restaurant_count > 0:
        a("-- Restaurant AI comparison / snapshots")
        a("WITH cfg AS (")
        a(
            f"  SELECT {sql_text_array(['점심 회전이 빠른 편입니다.', '분위기가 안정적인 회식용 매장입니다.', '혼밥 접근성이 좋은 편입니다.', '대화 중심 모임에 어울리는 공간입니다.', '대표 메뉴 만족도가 높습니다.', '야간 방문 수요가 꾸준한 편입니다.'])}::TEXT[] AS comparison_lines,"
        )
        a(
            f"         {sql_text_array(['매운맛 선호 수요가 높습니다.', '국물 메뉴 반응이 좋습니다.', '가성비 평가가 꾸준합니다.', '좌석 간격 만족도가 높습니다.', '주차 편의성 의견이 갈립니다.', '재방문 의사가 높은 편입니다.'])}::TEXT[] AS feature_lines"
        )
        a(")")
        a(
            "INSERT INTO restaurant_comparison "
            "(restaurant_id, model_version, comparison_json, analyzed_at)"
        )
        a("SELECT")
        a("  r.id,")
        a("  'dummy-v2',")
        a(
            "  jsonb_build_object("
            "'category_lift', jsonb_build_object("
            "'service', ROUND(((5 + (r.seq % 8))::numeric / 100), 2), "
            "'price', ROUND(((4 + (r.seq % 6))::numeric / 100), 2), "
            "'food', ROUND(((8 + (r.seq % 9))::numeric / 100), 2)"
            "), "
            "'comparison_display', jsonb_build_array(cfg.comparison_lines[((r.seq - 1) % CARDINALITY(cfg.comparison_lines)) + 1]), "
            "'total_candidates', 18 + (r.seq % 9), "
            "'validated_count', 11 + (r.seq % 7)"
            "),"
        )
        a("  NOW() - ((r.seq % 14) || ' days')::INTERVAL")
        a("FROM tmp_dummy_restaurant r")
        a("CROSS JOIN cfg")
        a("ON CONFLICT (restaurant_id) DO NOTHING;")
        a("")
        a("DO $$")
        a("BEGIN")
        a("  IF to_regclass('public.restaurant_ai_results') IS NOT NULL THEN")
        a(
            "    INSERT INTO restaurant_ai_results "
            "(id, restaurant_id, restaurant_name, summary_json, sentiment_json, comparison_json, errors_json)"
        )
        a("    SELECT")
        a("      COALESCE((SELECT MAX(id) FROM restaurant_ai_results), 0) + ROW_NUMBER() OVER (ORDER BY r.seq),")
        a("      r.id::INTEGER,")
        a("      rt.name,")
        a("      COALESCE(rs.summary_json::TEXT, '{}'::jsonb::TEXT),")
        a(
            "      jsonb_build_object("
            "'positive_percent', rse.positive_percent, "
            "'negative_percent', rse.negative_percent, "
            "'neutral_percent', rse.neutral_percent, "
            "'positive_count', rse.positive_count, "
            "'negative_count', rse.negative_count, "
            "'neutral_count', rse.neutral_count"
            ")::TEXT,"
        )
        a("      COALESCE(rc.comparison_json::TEXT, '{}'::jsonb::TEXT),")
        a("      CASE WHEN MOD(r.seq, 37) = 0 THEN '[\"vector sync pending\"]' ELSE '[]' END")
        a("    FROM tmp_dummy_restaurant r")
        a("    JOIN restaurant rt ON rt.id = r.id")
        a("    JOIN restaurant_review_summary rs ON rs.restaurant_id = r.id AND rs.vector_epoch = 0")
        a("    JOIN restaurant_review_sentiment rse ON rse.restaurant_id = r.id AND rse.vector_epoch = 0")
        a("    LEFT JOIN restaurant_comparison rc ON rc.restaurant_id = r.id")
        a("    WHERE NOT EXISTS (SELECT 1 FROM restaurant_ai_results existing WHERE existing.restaurant_id = r.id::INTEGER);")
        a("  END IF;")
        a("")
        a("  IF to_regclass('public.ai_restaurant_feature') IS NOT NULL THEN")
        a("    WITH cfg AS (")
        a(
            f"      SELECT {sql_text_array(['매운맛 선호 수요가 높습니다.', '국물 메뉴 반응이 좋습니다.', '가성비 평가가 꾸준합니다.', '좌석 간격 만족도가 높습니다.', '주차 편의성 의견이 갈립니다.', '재방문 의사가 높은 편입니다.'])}::TEXT[] AS feature_lines"
        )
        a("    )")
        a("    INSERT INTO ai_restaurant_feature (id, restaurant_id, content, created_at, updated_at)")
        a("    SELECT")
        a("      COALESCE((SELECT MAX(id) FROM ai_restaurant_feature), 0) + ROW_NUMBER() OVER (ORDER BY r.seq),")
        a("      r.id,")
        a(
            "      cfg.feature_lines[((r.seq - 1) % CARDINALITY(cfg.feature_lines)) + 1] || "
            "' · 리뷰 키워드 조합: ' || "
            "CASE MOD(r.seq, 4) "
            "  WHEN 0 THEN '친절함/맛있음/분위기 좋음' "
            "  WHEN 1 THEN '가성비 좋음/바로 입장/점심' "
            "  WHEN 2 THEN '대기 짧음/동료/저녁' "
            "  ELSE '혼밥/데이트/회식' "
            "END,"
        )
        a("      NOW(),")
        a("      NOW()")
        a("    FROM tmp_dummy_restaurant r")
        a("    CROSS JOIN cfg")
        a("    WHERE NOT EXISTS (SELECT 1 FROM ai_restaurant_feature existing WHERE existing.restaurant_id = r.id);")
        a("  END IF;")
        a("END $$;")
        a("")

    if effective_report_count > 0:
        a("-- Reports")
        a("WITH cfg AS (")
        a(
            f"  SELECT {sql_text_array(['BUG', 'INAPPROPRIATE_REVIEW', 'INAPPROPRIATE_CONTENT', 'RESTAURANT_INFO', 'SPAM', 'OTHER'])}::TEXT[] AS categories,"
        )
        a(
            f"         {sql_text_array(['PENDING', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'])}::TEXT[] AS statuses"
        )
        a(")")
        a("INSERT INTO report (member_id, category, content, status, created_at, updated_at)")
        a("SELECT")
        a("  m.id,")
        a("  cfg.categories[(rep.seq % CARDINALITY(cfg.categories)) + 1],")
        a(
            "  CASE cfg.categories[(rep.seq % CARDINALITY(cfg.categories)) + 1] "
            "    WHEN 'BUG' THEN '시드신고-" + run_token + "- 앱 홈에서 추천 섹션 정렬이 기대와 다르게 보입니다.' "
            "    WHEN 'INAPPROPRIATE_REVIEW' THEN '시드신고-" + run_token + "- 광고성 문구가 과도한 리뷰가 보여 확인이 필요합니다.' "
            "    WHEN 'INAPPROPRIATE_CONTENT' THEN '시드신고-" + run_token + "- 채팅 내 부적절 표현 신고 테스트용 데이터입니다.' "
            "    WHEN 'RESTAURANT_INFO' THEN '시드신고-" + run_token + "- 영업시간 또는 주소 정보가 실제와 다를 수 있어 검토가 필요합니다.' "
            "    WHEN 'SPAM' THEN '시드신고-" + run_token + "- 반복성 홍보성 활동이 감지되어 차단 정책 검토가 필요합니다.' "
            "    ELSE '시드신고-" + run_token + "- 기타 운영 확인이 필요한 이슈를 기록한 테스트 데이터입니다.' "
            "  END,"
        )
        a("  CASE WHEN MOD(rep.seq, 11) = 0 THEN 'REJECTED' WHEN MOD(rep.seq, 7) = 0 THEN 'RESOLVED' WHEN MOD(rep.seq, 5) = 0 THEN 'IN_PROGRESS' ELSE 'PENDING' END,")
        a("  NOW() - ((rep.seq % 45) || ' days')::INTERVAL,")
        a("  NOW() - ((rep.seq % 45) || ' days')::INTERVAL + ((rep.seq % 180) || ' minutes')::INTERVAL")
        a(f"FROM generate_series(0, {effective_report_count - 1}) AS rep(seq)")
        a("JOIN tmp_dummy_member m")
        a(f"  ON m.seq = ((rep.seq % {member_count}) + 1)")
        a("CROSS JOIN cfg;")
        a("")

    if effective_restaurant_image_count > 0:
        a("-- Restaurant images / domain images")
        a(
            "CREATE TEMP TABLE tmp_restaurant_domain_image "
            "(seq INTEGER PRIMARY KEY, restaurant_id BIGINT NOT NULL, image_id BIGINT NOT NULL, image_url TEXT NOT NULL, sort_order INTEGER NOT NULL, file_uuid UUID NOT NULL) ON COMMIT DROP;"
        )
        a("INSERT INTO tmp_restaurant_domain_image (seq, restaurant_id, image_id, image_url, sort_order, file_uuid)")
        a("SELECT")
        a("  seeded.rn,")
        a("  seeded.restaurant_id,")
        a("  seeded.image_id,")
        a("  seeded.image_url,")
        a("  seeded.sort_order,")
        a("  seeded.file_uuid")
        a("FROM (")
        a("  SELECT")
        a("    ROW_NUMBER() OVER (ORDER BY r.seq, img.idx) AS rn,")
        a("    r.id AS restaurant_id,")
        a("    nextval('image_seq') AS image_id,")
        a(
            "    "
            + sql_asset_url_expr(
                content["restaurant_image_base_url"],
                run_token,
                "'restaurant/' || r.id || '/gallery-' || (img.idx - 1)",
                "webp",
                sql_quote(run_token) + " || '-restaurant-image-' || r.id::TEXT || '-' || img.idx::TEXT",
                [
                    "w=720&q=76&fit=cover",
                    "w=1280&q=82&fit=cover",
                    "w=1600&q=86&fit=cover",
                ],
            )
            + " AS image_url,"
        )
        a("    img.idx - 1 AS sort_order,")
        a(
            "    "
            + sql_uuid_expr(sql_quote(run_token) + " || '-restaurant-image-' || r.id::TEXT || '-' || img.idx::TEXT")
            + " AS file_uuid"
        )
        a("  FROM tmp_dummy_restaurant r")
        a("  CROSS JOIN generate_series(1, 3) AS img(idx)")
        a(") seeded")
        a(f"WHERE seeded.rn <= {effective_restaurant_image_count};")
        a("")
        a(
            "INSERT INTO image "
            "(id, file_name, file_size, file_type, storage_key, file_uuid, status, purpose, created_at, updated_at)"
        )
        a("SELECT")
        a("  image_id,")
        a(
            "  'restaurant-' || restaurant_id || '-' || (sort_order + 1) || '-' || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-restaurant-image-file-' || restaurant_id::TEXT || '-' || sort_order::TEXT",
                1,
                6,
            )
            + " || '.webp',"
        )
        a("  180000 + ((seq % 20) * 4000),")
        a("  'image/webp',")
        a(
            "  "
            + sql_storage_key_expr(
                "uploads/restaurant/image",
                run_token,
                "'restaurant/' || restaurant_id || '/gallery-' || (sort_order + 1)",
                "webp",
                sql_quote(run_token) + " || '-restaurant-image-storage-' || restaurant_id::TEXT || '-' || sort_order::TEXT",
            )
            + ","
        )
        a("  file_uuid,")
        a("  'ACTIVE',")
        a("  'RESTAURANT_IMAGE',")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_restaurant_domain_image;")
        a("")
        a("INSERT INTO domain_image (id, domain_type, domain_id, image_id, sort_order, created_at)")
        a("SELECT")
        a("  nextval('domain_image_seq'),")
        a("  'RESTAURANT',")
        a("  restaurant_id,")
        a("  image_id,")
        a("  sort_order,")
        a("  NOW()")
        a("FROM tmp_restaurant_domain_image")
        a("ON CONFLICT ON CONSTRAINT uq_domain_image_link DO NOTHING;")
        a("")
        a("DO $$")
        a("BEGIN")
        a("  IF to_regclass('public.restaurant_image') IS NOT NULL THEN")
        a(
            "    INSERT INTO restaurant_image "
            "(id, restaurant_id, image_url, sort_order, deleted_at, created_at)"
        )
        a("    SELECT")
        a("      COALESCE((SELECT MAX(id) FROM restaurant_image), 0) + seq,")
        a("      restaurant_id,")
        a("      image_url,")
        a("      sort_order,")
        a("      NULL,")
        a("      NOW()")
        a("    FROM tmp_restaurant_domain_image;")
        a("  END IF;")
        a("END $$;")
        a("")

    if effective_review_image_count > 0:
        a("-- Review images / domain images")
        a(
            "CREATE TEMP TABLE tmp_review_domain_image "
            "(seq INTEGER PRIMARY KEY, review_id BIGINT NOT NULL, image_id BIGINT NOT NULL, image_url TEXT NOT NULL, file_uuid UUID NOT NULL) ON COMMIT DROP;"
        )
        a("INSERT INTO tmp_review_domain_image (seq, review_id, image_id, image_url, file_uuid)")
        a("SELECT")
        a("  rv.seq,")
        a("  rv.id,")
        a("  nextval('image_seq'),")
        a(
            "  "
            + sql_asset_url_expr(
                content["review_image_base_url"],
                run_token,
                "'review/' || rv.id",
                "webp",
                sql_quote(run_token) + " || '-review-image-' || rv.id::TEXT",
                [
                    "w=640&q=74&fit=cover",
                    "w=1080&q=80&fit=cover",
                    "w=1440&q=84&fit=cover",
                ],
            )
            + ","
        )
        a(
            "  "
            + sql_uuid_expr(sql_quote(run_token) + " || '-review-image-' || rv.id::TEXT")
        )
        a("FROM tmp_dummy_review rv")
        a(f"WHERE rv.seq <= {effective_review_image_count};")
        a("")
        a(
            "INSERT INTO image "
            "(id, file_name, file_size, file_type, storage_key, file_uuid, status, purpose, created_at, updated_at)"
        )
        a("SELECT")
        a("  image_id,")
        a(
            "  'review-' || review_id || '-' || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-review-image-file-' || review_id::TEXT",
                1,
                6,
            )
            + " || '.webp',"
        )
        a("  120000 + ((seq % 15) * 3000),")
        a("  'image/webp',")
        a(
            "  "
            + sql_storage_key_expr(
                "uploads/review/image",
                run_token,
                "'review/' || review_id",
                "webp",
                sql_quote(run_token) + " || '-review-image-storage-' || review_id::TEXT",
            )
            + ","
        )
        a("  file_uuid,")
        a("  'ACTIVE',")
        a("  'REVIEW_IMAGE',")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_review_domain_image;")
        a("")
        a("INSERT INTO domain_image (id, domain_type, domain_id, image_id, sort_order, created_at)")
        a("SELECT")
        a("  nextval('domain_image_seq'),")
        a("  'REVIEW',")
        a("  review_id,")
        a("  image_id,")
        a("  0,")
        a("  NOW()")
        a("FROM tmp_review_domain_image")
        a("ON CONFLICT ON CONSTRAINT uq_domain_image_link DO NOTHING;")
        a("")
        a("DO $$")
        a("BEGIN")
        a("  IF to_regclass('public.review_image') IS NOT NULL THEN")
        a("    INSERT INTO review_image (id, review_id, image_url, deleted_at, created_at)")
        a("    SELECT")
        a("      COALESCE((SELECT MAX(id) FROM review_image), 0) + seq,")
        a("      review_id,")
        a("      image_url,")
        a("      NULL,")
        a("      NOW()")
        a("    FROM tmp_review_domain_image;")
        a("  END IF;")
        a("END $$;")
        a("")

    if effective_member_image_count > 0:
        a("-- Member profile images / domain images")
        a(
            "CREATE TEMP TABLE tmp_member_domain_image "
            "(seq INTEGER PRIMARY KEY, member_id BIGINT NOT NULL, image_id BIGINT NOT NULL, file_uuid UUID NOT NULL) ON COMMIT DROP;"
        )
        a("INSERT INTO tmp_member_domain_image (seq, member_id, image_id, file_uuid)")
        a("SELECT")
        a("  m.seq,")
        a("  m.id,")
        a("  nextval('image_seq'),")
        a(
            "  "
            + sql_uuid_expr(sql_quote(run_token) + " || '-member-image-' || m.id::TEXT")
        )
        a("FROM tmp_dummy_member m")
        a(f"WHERE m.seq <= {effective_member_image_count};")
        a("")
        a(
            "INSERT INTO image "
            "(id, file_name, file_size, file_type, storage_key, file_uuid, status, purpose, created_at, updated_at)"
        )
        a("SELECT")
        a("  image_id,")
        a(
            "  'member-' || member_id || '-' || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-member-image-file-' || member_id::TEXT",
                1,
                6,
            )
            + " || '.webp',"
        )
        a("  96000 + ((seq % 10) * 2000),")
        a("  'image/webp',")
        a(
            "  "
            + sql_storage_key_expr(
                "uploads/member/profile",
                run_token,
                "'member/' || member_id",
                "webp",
                sql_quote(run_token) + " || '-member-image-storage-' || member_id::TEXT",
            )
            + ","
        )
        a("  file_uuid,")
        a("  'ACTIVE',")
        a("  'PROFILE_IMAGE',")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_member_domain_image;")
        a("")
        a("INSERT INTO domain_image (id, domain_type, domain_id, image_id, sort_order, created_at)")
        a("SELECT nextval('domain_image_seq'), 'MEMBER', member_id, image_id, 0, NOW() FROM tmp_member_domain_image")
        a("ON CONFLICT ON CONSTRAINT uq_domain_image_link DO NOTHING;")
        a("")

    if effective_group_image_count > 0:
        a("-- Group images / domain images")
        a(
            "CREATE TEMP TABLE tmp_group_domain_image "
            "(seq INTEGER PRIMARY KEY, group_id BIGINT NOT NULL, image_id BIGINT NOT NULL, file_uuid UUID NOT NULL) ON COMMIT DROP;"
        )
        a("INSERT INTO tmp_group_domain_image (seq, group_id, image_id, file_uuid)")
        a("SELECT")
        a("  g.seq,")
        a("  g.id,")
        a("  nextval('image_seq'),")
        a(
            "  "
            + sql_uuid_expr(sql_quote(run_token) + " || '-group-image-' || g.id::TEXT")
        )
        a("FROM tmp_dummy_group g")
        a(f"WHERE g.seq <= {effective_group_image_count};")
        a("")
        a(
            "INSERT INTO image "
            "(id, file_name, file_size, file_type, storage_key, file_uuid, status, purpose, created_at, updated_at)"
        )
        a("SELECT")
        a("  image_id,")
        a(
            "  'group-' || group_id || '-' || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-group-image-file-' || group_id::TEXT",
                1,
                6,
            )
            + " || '.png',"
        )
        a("  140000 + ((seq % 12) * 2500),")
        a("  'image/png',")
        a(
            "  "
            + sql_storage_key_expr(
                "uploads/group/image",
                run_token,
                "'group/' || group_id",
                "png",
                sql_quote(run_token) + " || '-group-image-storage-' || group_id::TEXT",
            )
            + ","
        )
        a("  file_uuid,")
        a("  'ACTIVE',")
        a("  'GROUP_IMAGE',")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_group_domain_image;")
        a("")
        a("INSERT INTO domain_image (id, domain_type, domain_id, image_id, sort_order, created_at)")
        a("SELECT nextval('domain_image_seq'), 'GROUP', group_id, image_id, 0, NOW() FROM tmp_group_domain_image")
        a("ON CONFLICT ON CONSTRAINT uq_domain_image_link DO NOTHING;")
        a("")

    if effective_subgroup_image_count > 0:
        a("-- Subgroup images / domain images")
        a(
            "CREATE TEMP TABLE tmp_subgroup_domain_image "
            "(seq INTEGER PRIMARY KEY, subgroup_id BIGINT NOT NULL, image_id BIGINT NOT NULL, file_uuid UUID NOT NULL) ON COMMIT DROP;"
        )
        a("INSERT INTO tmp_subgroup_domain_image (seq, subgroup_id, image_id, file_uuid)")
        a("SELECT")
        a("  sg.seq,")
        a("  sg.id,")
        a("  nextval('image_seq'),")
        a(
            "  "
            + sql_uuid_expr(sql_quote(run_token) + " || '-subgroup-image-' || sg.id::TEXT")
        )
        a("FROM tmp_dummy_subgroup sg")
        a(f"WHERE sg.seq <= {effective_subgroup_image_count};")
        a("")
        a(
            "INSERT INTO image "
            "(id, file_name, file_size, file_type, storage_key, file_uuid, status, purpose, created_at, updated_at)"
        )
        a("SELECT")
        a("  image_id,")
        a(
            "  'subgroup-' || subgroup_id || '-' || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-subgroup-image-file-' || subgroup_id::TEXT",
                1,
                6,
            )
            + " || '.jpg',"
        )
        a("  128000 + ((seq % 12) * 2400),")
        a("  'image/jpeg',")
        a(
            "  "
            + sql_storage_key_expr(
                "uploads/subgroup/image",
                run_token,
                "'subgroup/' || subgroup_id",
                "jpg",
                sql_quote(run_token) + " || '-subgroup-image-storage-' || subgroup_id::TEXT",
            )
            + ","
        )
        a("  file_uuid,")
        a("  'ACTIVE',")
        a("  'GROUP_IMAGE',")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_subgroup_domain_image;")
        a("")
        a("INSERT INTO domain_image (id, domain_type, domain_id, image_id, sort_order, created_at)")
        a("SELECT nextval('domain_image_seq'), 'SUBGROUP', subgroup_id, image_id, 0, NOW() FROM tmp_subgroup_domain_image")
        a("ON CONFLICT ON CONSTRAINT uq_domain_image_link DO NOTHING;")
        a("")

    if effective_menu_domain_image_count > 0:
        a("-- Menu domain images")
        a(
            "CREATE TEMP TABLE tmp_menu_domain_image "
            "(seq INTEGER PRIMARY KEY, menu_id BIGINT NOT NULL, image_id BIGINT NOT NULL, file_uuid UUID NOT NULL) ON COMMIT DROP;"
        )
        a("INSERT INTO tmp_menu_domain_image (seq, menu_id, image_id, file_uuid)")
        a("SELECT")
        a("  seeded.rn,")
        a("  seeded.menu_id,")
        a("  nextval('image_seq'),")
        a(
            "  "
            + sql_uuid_expr(sql_quote(run_token) + " || '-menu-image-' || seeded.menu_id::TEXT")
        )
        a("FROM (")
        a("  SELECT m.id AS menu_id, ROW_NUMBER() OVER (ORDER BY m.id) AS rn")
        a("  FROM menu m")
        a("  JOIN tmp_dummy_menu_category mc ON mc.id = m.category_id")
        a(") seeded")
        a(f"WHERE seeded.rn <= {effective_menu_domain_image_count};")
        a("")
        a(
            "INSERT INTO image "
            "(id, file_name, file_size, file_type, storage_key, file_uuid, status, purpose, created_at, updated_at)"
        )
        a("SELECT")
        a("  image_id,")
        a(
            "  'menu-' || menu_id || '-' || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-menu-image-file-' || menu_id::TEXT",
                1,
                6,
            )
            + " || '.jpg',"
        )
        a("  110000 + ((seq % 10) * 2100),")
        a("  'image/jpeg',")
        a(
            "  "
            + sql_storage_key_expr(
                "uploads/menu/image",
                run_token,
                "'menu/' || menu_id",
                "jpg",
                sql_quote(run_token) + " || '-menu-image-storage-' || menu_id::TEXT",
            )
            + ","
        )
        a("  file_uuid,")
        a("  'ACTIVE',")
        a("  'MENU_IMAGE',")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_menu_domain_image;")
        a("")
        a("INSERT INTO domain_image (id, domain_type, domain_id, image_id, sort_order, created_at)")
        a("SELECT nextval('domain_image_seq'), 'MENU', menu_id, image_id, 0, NOW() FROM tmp_menu_domain_image")
        a("ON CONFLICT ON CONSTRAINT uq_domain_image_link DO NOTHING;")
        a("")

    if effective_chat_image_count > 0:
        a("-- Chat message images / files")
        a(
            "CREATE TEMP TABLE tmp_chat_domain_image "
            "(seq INTEGER PRIMARY KEY, chat_message_id BIGINT NOT NULL, image_id BIGINT NOT NULL, image_url TEXT NOT NULL, file_uuid UUID NOT NULL) ON COMMIT DROP;"
        )
        a("INSERT INTO tmp_chat_domain_image (seq, chat_message_id, image_id, image_url, file_uuid)")
        a("SELECT")
        a("  seeded.rn,")
        a("  seeded.chat_message_id,")
        a("  nextval('image_seq'),")
        a(
            "  "
            + sql_asset_url_expr(
                content["chat_image_base_url"],
                run_token,
                "'chat/' || seeded.chat_message_id",
                "webp",
                sql_quote(run_token) + " || '-chat-image-' || seeded.chat_message_id::TEXT",
                [
                    "w=480&q=72&fit=cover",
                    "w=960&q=78&fit=cover",
                    "w=1280&q=82&fit=cover",
                ],
            )
            + ","
        )
        a(
            "  "
            + sql_uuid_expr(sql_quote(run_token) + " || '-chat-image-' || seeded.chat_message_id::TEXT")
        )
        a("FROM (")
        a("  SELECT cm.id AS chat_message_id, ROW_NUMBER() OVER (ORDER BY cm.id) AS rn")
        a("  FROM chat_message cm")
        a("  JOIN tmp_dummy_chat_room cr ON cr.id = cm.chat_room_id")
        a(") seeded")
        a(f"WHERE seeded.rn <= {effective_chat_image_count};")
        a("")
        a(
            "INSERT INTO image "
            "(id, file_name, file_size, file_type, storage_key, file_uuid, status, purpose, created_at, updated_at)"
        )
        a("SELECT")
        a("  image_id,")
        a(
            "  'chat-' || chat_message_id || '-' || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-chat-image-file-' || chat_message_id::TEXT",
                1,
                6,
            )
            + " || '.webp',"
        )
        a("  84000 + ((seq % 8) * 1800),")
        a("  'image/webp',")
        a(
            "  "
            + sql_storage_key_expr(
                "uploads/chat/image",
                run_token,
                "'chat/' || chat_message_id",
                "webp",
                sql_quote(run_token) + " || '-chat-image-storage-' || chat_message_id::TEXT",
            )
            + ","
        )
        a("  file_uuid,")
        a("  'ACTIVE',")
        a("  'CHAT_IMAGE',")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_chat_domain_image;")
        a("")
        a("DO $$")
        a("BEGIN")
        a("  IF EXISTS (")
        a(
            "    SELECT 1 FROM pg_constraint c"
            " WHERE c.conrelid = 'domain_image'::regclass"
            "   AND c.conname = 'domain_image_domain_type_check'"
            "   AND c.contype = 'c'"
            "   AND pg_get_constraintdef(c.oid) LIKE '%CHAT_MESSAGE%'"
        )
        a("  ) THEN")
        a("    INSERT INTO domain_image (id, domain_type, domain_id, image_id, sort_order, created_at)")
        a("    SELECT nextval('domain_image_seq'), 'CHAT_MESSAGE', chat_message_id, image_id, 0, NOW()")
        a("    FROM tmp_chat_domain_image")
        a("    ON CONFLICT ON CONSTRAINT uq_domain_image_link DO NOTHING;")
        a("  END IF;")
        a("END $$;")
        a("")
        a(
            "INSERT INTO chat_message_file "
            "(id, chat_message_id, file_type, file_url, created_at, deleted_at, file_uuid, domain_image_id)"
        )
        a("SELECT")
        a("  nextval('chat_message_file_id_seq'),")
        a("  c.chat_message_id,")
        a("  'IMAGE',")
        a("  c.image_url,")
        a("  NOW(),")
        a("  NULL,")
        a("  c.file_uuid::TEXT,")
        a("  CASE")
        a("    WHEN EXISTS (")
        a(
            "      SELECT 1 FROM pg_constraint c"
            " WHERE c.conrelid = 'domain_image'::regclass"
            "   AND c.conname = 'domain_image_domain_type_check'"
            "   AND c.contype = 'c'"
            "   AND pg_get_constraintdef(c.oid) LIKE '%CHAT_MESSAGE%'"
        )
        a("    ) THEN d.id")
        a("    ELSE NULL")
        a("  END AS domain_image_id")
        a("FROM tmp_chat_domain_image c")
        a("LEFT JOIN domain_image d")
        a("  ON d.domain_type = 'CHAT_MESSAGE'")
        a(" AND d.domain_id = c.chat_message_id")
        a(" AND d.image_id = c.image_id;")
        a("")

    if effective_promotion_count > 0:
        a("-- Promotion common assets / domain images")
        a(
            "CREATE TEMP TABLE tmp_promotion_domain_image "
            "(seq INTEGER PRIMARY KEY, promotion_id BIGINT NOT NULL, image_id BIGINT NOT NULL, image_url TEXT NOT NULL, sort_order INTEGER NOT NULL, file_uuid UUID NOT NULL) ON COMMIT DROP;"
        )
        a("INSERT INTO tmp_promotion_domain_image (seq, promotion_id, image_id, image_url, sort_order, file_uuid)")
        a("SELECT")
        a("  asset.seq,")
        a("  asset.promotion_id,")
        a("  nextval('image_seq'),")
        a("  asset.image_url,")
        a("  asset.sort_order,")
        a(
            "  "
            + sql_uuid_expr(sql_quote(run_token) + " || '-promotion-image-' || asset.promotion_id::TEXT || '-' || asset.asset_type")
        )
        a("FROM tmp_dummy_promotion_asset asset;")
        a("")
        a(
            "INSERT INTO image "
            "(id, file_name, file_size, file_type, storage_key, file_uuid, status, purpose, created_at, updated_at)"
        )
        a("SELECT")
        a("  image_id,")
        a(
            "  'promotion-' || promotion_id || '-' || (sort_order + 1) || '-' || "
            + sql_hash_slice_expr(
                sql_quote(run_token) + " || '-promotion-image-file-' || promotion_id::TEXT || '-' || sort_order::TEXT",
                1,
                6,
            )
            + " || '.webp',"
        )
        a("  210000 + ((seq % 10) * 5000),")
        a("  'image/webp',")
        a(
            "  "
            + sql_storage_key_expr(
                "uploads/promotion/asset",
                run_token,
                "'promotion/' || promotion_id || '/asset-' || (sort_order + 1)",
                "webp",
                sql_quote(run_token) + " || '-promotion-image-storage-' || promotion_id::TEXT || '-' || sort_order::TEXT",
            )
            + ","
        )
        a("  file_uuid,")
        a("  'ACTIVE',")
        a("  'COMMON_ASSET',")
        a("  NOW(),")
        a("  NOW()")
        a("FROM tmp_promotion_domain_image;")
        a("")
        a("INSERT INTO domain_image (id, domain_type, domain_id, image_id, sort_order, created_at)")
        a("SELECT nextval('domain_image_seq'), 'PROMOTION', promotion_id, image_id, sort_order, NOW() FROM tmp_promotion_domain_image")
        a("ON CONFLICT ON CONSTRAINT uq_domain_image_link DO NOTHING;")
        a("")

    if effective_image_optimization_job_count > 0:
        a("-- Image optimization jobs")
        a("DO $$")
        a("BEGIN")
        a("  IF to_regclass('public.image_optimization_job') IS NOT NULL THEN")
        a(
            "    INSERT INTO image_optimization_job "
            "(image_id, original_width, original_height, optimized_width, optimized_height, original_size, optimized_size, processed_at, status, error_message, created_at)"
        )
        a("    SELECT")
        a("      seeded.image_id,")
        a("      1600 - ((seeded.rn % 3) * 160),")
        a("      1200 - ((seeded.rn % 4) * 120),")
        a("      CASE WHEN MOD(seeded.rn, 5) = 0 THEN NULL ELSE 1280 - ((seeded.rn % 3) * 120) END,")
        a("      CASE WHEN MOD(seeded.rn, 5) = 0 THEN NULL ELSE 960 - ((seeded.rn % 4) * 90) END,")
        a("      seeded.file_size,")
        a("      CASE WHEN MOD(seeded.rn, 11) = 0 THEN NULL ELSE (seeded.file_size * 72 / 100) END,")
        a("      CASE WHEN MOD(seeded.rn, 5) = 0 THEN NULL ELSE NOW() - ((seeded.rn % 20) || ' hours')::INTERVAL END,")
        a("      CASE WHEN MOD(seeded.rn, 11) = 0 THEN 'FAILED' WHEN MOD(seeded.rn, 5) = 0 THEN 'PENDING' ELSE 'SUCCESS' END,")
        a("      CASE WHEN MOD(seeded.rn, 11) = 0 THEN '원본 메타데이터가 부족해 최적화가 지연되었습니다.' ELSE NULL END,")
        a("      NOW() - ((seeded.rn % 7) || ' days')::INTERVAL")
        a("    FROM (")
        a("      SELECT i.id AS image_id, i.file_size, ROW_NUMBER() OVER (ORDER BY i.id) AS rn")
        a("      FROM image i")
        a(
            "      WHERE i.storage_key LIKE '%/"
            + run_token
            + "/%'"
        )
        a("    ) seeded")
        a(f"    WHERE seeded.rn <= {effective_image_optimization_job_count}")
        a("    ON CONFLICT (image_id) DO NOTHING;")
        a("  END IF;")
        a("END $$;")
        a("")

    if include_async_log_data and (
        total_user_activity_event_count > 0
        or effective_notification_outbox_count > 0
        or effective_user_activity_group_join_count > 0
    ):
        a("-- Async pipeline / operational log seed")

        if total_user_activity_event_count > 0:
            a(
                "CREATE TEMP TABLE tmp_async_user_activity_event ("
                "seq INTEGER PRIMARY KEY, event_id VARCHAR(64) NOT NULL UNIQUE, event_name VARCHAR(100) NOT NULL, "
                "event_version VARCHAR(20) NOT NULL, occurred_at TIMESTAMPTZ NOT NULL, member_id BIGINT, "
                "anonymous_id VARCHAR(100), session_id VARCHAR(100), source VARCHAR(20) NOT NULL, "
                "request_path VARCHAR(255), request_method VARCHAR(10), device_id VARCHAR(100), "
                "platform VARCHAR(30), app_version VARCHAR(30), locale VARCHAR(20), properties JSONB NOT NULL"
                ") ON COMMIT DROP;"
            )
            a("")

            ua_offset = 0

            if effective_user_activity_group_join_count > 0:
                a("-- User activity: group.joined")
                a(
                    "INSERT INTO tmp_async_user_activity_event "
                    "(seq, event_id, event_name, event_version, occurred_at, member_id, anonymous_id, session_id, "
                    "source, request_path, request_method, device_id, platform, app_version, locale, properties)"
                )
                a("SELECT")
                a(f"  {ua_offset} + seeded.rn,")
                a(
                    "  'ua-"
                    + run_token
                    + "-group-joined-' || LPAD(seeded.rn::TEXT, 8, '0'),"
                )
                a("  'group.joined',")
                a("  'v1',")
                a("  NOW() - ((seeded.rn % 45) || ' days')::INTERVAL - ((seeded.rn % 720) || ' minutes')::INTERVAL,")
                a("  seeded.member_id,")
                a("  NULL,")
                a("  NULL,")
                a("  'SERVER',")
                a("  '/groups/' || seeded.group_id,")
                a("  NULL,")
                a("  NULL,")
                a("  NULL,")
                a("  NULL,")
                a("  'ko-KR',")
                a("  jsonb_build_object('groupId', seeded.group_id, 'groupName', seeded.group_name)")
                a("FROM (")
                a("  SELECT gm.member_id, gm.group_id, g.name AS group_name, ROW_NUMBER() OVER (ORDER BY gm.id) AS rn")
                a("  FROM group_member gm")
                a("  JOIN tmp_dummy_group tg ON tg.id = gm.group_id")
                a("  JOIN \"group\" g ON g.id = gm.group_id")
                a(") seeded")
                a(f"WHERE seeded.rn <= {effective_user_activity_group_join_count};")
                a("")
                ua_offset += effective_user_activity_group_join_count

            if effective_user_activity_review_created_count > 0:
                a("-- User activity: review.created")
                a(
                    "INSERT INTO tmp_async_user_activity_event "
                    "(seq, event_id, event_name, event_version, occurred_at, member_id, anonymous_id, session_id, "
                    "source, request_path, request_method, device_id, platform, app_version, locale, properties)"
                )
                a("SELECT")
                a(f"  {ua_offset} + rv.seq,")
                a(
                    "  'ua-"
                    + run_token
                    + "-review-created-' || LPAD(rv.seq::TEXT, 8, '0'),"
                )
                a("  'review.created',")
                a("  'v1',")
                a("  NOW() - ((rv.seq % 30) || ' days')::INTERVAL - ((rv.seq % 480) || ' minutes')::INTERVAL,")
                a("  NULL,")
                a("  NULL,")
                a("  NULL,")
                a("  'SERVER',")
                a("  NULL,")
                a("  NULL,")
                a("  NULL,")
                a("  NULL,")
                a("  NULL,")
                a("  'ko-KR',")
                a("  jsonb_build_object('restaurantId', rv.restaurant_id)")
                a("FROM tmp_dummy_review rv")
                a(f"WHERE rv.seq <= {effective_user_activity_review_created_count};")
                a("")
                ua_offset += effective_user_activity_review_created_count

            if effective_user_activity_search_count > 0:
                a("-- User activity: ui.search.executed")
                a("WITH cfg AS (")
                a(
                    f"  SELECT {sql_text_array(content['search_keywords'])}::TEXT[] AS keywords,"
                    f" {sql_text_array(['IOS', 'ANDROID', 'WEB'])}::TEXT[] AS platforms"
                )
                a(")")
                a(
                    "INSERT INTO tmp_async_user_activity_event "
                    "(seq, event_id, event_name, event_version, occurred_at, member_id, anonymous_id, session_id, "
                    "source, request_path, request_method, device_id, platform, app_version, locale, properties)"
                )
                a("SELECT")
                a(f"  {ua_offset} + se.seq,")
                a(
                    "  'ua-"
                    + run_token
                    + "-search-' || LPAD(se.seq::TEXT, 8, '0'),"
                )
                a("  'ui.search.executed',")
                a("  'v1',")
                a("  NOW() - ((se.seq % 20) || ' days')::INTERVAL - ((se.seq % 1440) || ' minutes')::INTERVAL,")
                a("  m.id,")
                a("  NULL,")
                a(
                    "  SUBSTR(MD5("
                    + sql_quote(run_token)
                    + " || '-search-session-' || m.id::TEXT || '-' || se.seq::TEXT), 1, 24),"
                )
                a("  'CLIENT',")
                a("  '/search',")
                a("  'GET',")
                a(
                    "  'device-' || "
                    + sql_quote(run_token)
                    + " || '-slot-' || LPAD(("
                    + sql_bucket_expr(
                        sql_quote(run_token) + " || '-ua-device-' || m.id::TEXT",
                        1,
                        24,
                        1,
                    )
                    + ")::TEXT, 2, '0') || '-' || "
                    + sql_hash_slice_expr(
                        sql_quote(run_token) + " || '-ua-device-' || m.id::TEXT",
                        1,
                        16,
                    )
                    + ","
                )
                a("  cfg.platforms[((se.seq - 1) % CARDINALITY(cfg.platforms)) + 1],")
                a("  '2.' || ((se.seq % 4) + 1) || '.' || ((se.seq % 10) + 1),")
                a("  'ko-KR',")
                a(
                    "  jsonb_build_object("
                    "'keyword', cfg.keywords[((se.seq - 1) % CARDINALITY(cfg.keywords)) + 1], "
                    "'resultCount', 5 + (se.seq % 18), "
                    "'source', 'CLIENT'"
                    ")"
                )
                a(f"FROM generate_series(1, {effective_user_activity_search_count}) AS se(seq)")
                a("JOIN tmp_dummy_member m")
                a(f"  ON m.seq = (((se.seq - 1) % {member_count}) + 1)")
                a("CROSS JOIN cfg;")
                a("")
                ua_offset += effective_user_activity_search_count

            if effective_user_activity_favorite_count > 0:
                a("-- User activity: ui.favorite.updated")
                a("WITH cfg AS (")
                a(f"  SELECT {sql_text_array(['IOS', 'ANDROID', 'WEB'])}::TEXT[] AS platforms")
                a(")")
                a(
                    "INSERT INTO tmp_async_user_activity_event "
                    "(seq, event_id, event_name, event_version, occurred_at, member_id, anonymous_id, session_id, "
                    "source, request_path, request_method, device_id, platform, app_version, locale, properties)"
                )
                a("SELECT")
                a(f"  {ua_offset} + fs.seq + 1,")
                a(
                    "  'ua-"
                    + run_token
                    + "-favorite-updated-' || LPAD((fs.seq + 1)::TEXT, 8, '0'),"
                )
                a("  'ui.favorite.updated',")
                a("  'v1',")
                a("  NOW() - ((fs.seq % 25) || ' days')::INTERVAL - ((fs.seq % 900) || ' minutes')::INTERVAL,")
                a("  m.id,")
                a("  NULL,")
                a(
                    "  SUBSTR(MD5("
                    + sql_quote(run_token)
                    + " || '-favorite-session-' || m.id::TEXT || '-' || r.id::TEXT), 1, 24),"
                )
                a("  'CLIENT',")
                a("  '/restaurants/' || r.id || '/favorite',")
                a("  'POST',")
                a(
                    "  'device-' || "
                    + sql_quote(run_token)
                    + " || '-slot-' || LPAD(("
                    + sql_bucket_expr(
                        sql_quote(run_token) + " || '-ua-device-' || m.id::TEXT",
                        1,
                        24,
                        1,
                    )
                    + ")::TEXT, 2, '0') || '-' || "
                    + sql_hash_slice_expr(
                        sql_quote(run_token) + " || '-ua-device-' || m.id::TEXT",
                        1,
                        16,
                    )
                    + ","
                )
                a("  cfg.platforms[((fs.seq) % CARDINALITY(cfg.platforms)) + 1],")
                a("  '2.' || ((fs.seq % 4) + 1) || '.' || ((fs.seq % 10) + 1),")
                a("  'ko-KR',")
                a("  jsonb_build_object('restaurantId', r.id, 'action', 'ADD', 'source', 'CLIENT')")
                a(f"FROM generate_series(0, {effective_user_activity_favorite_count - 1}) AS fs(seq)")
                a("JOIN tmp_dummy_member m")
                a(f"  ON m.seq = ((fs.seq % {member_count}) + 1)")
                a("JOIN tmp_dummy_restaurant r")
                a(f"  ON r.seq = (((fs.seq / {member_count}) % {restaurant_count}) + 1)")
                a("CROSS JOIN cfg;")
                a("")
                ua_offset += effective_user_activity_favorite_count

            if effective_user_activity_restaurant_view_count > 0:
                a("-- User activity: ui.restaurant.viewed")
                a("WITH cfg AS (")
                a(f"  SELECT {sql_text_array(['IOS', 'ANDROID', 'WEB'])}::TEXT[] AS platforms")
                a(")")
                a(
                    "INSERT INTO tmp_async_user_activity_event "
                    "(seq, event_id, event_name, event_version, occurred_at, member_id, anonymous_id, session_id, "
                    "source, request_path, request_method, device_id, platform, app_version, locale, properties)"
                )
                a("SELECT")
                a(f"  {ua_offset} + rvw.seq,")
                a(
                    "  'ua-"
                    + run_token
                    + "-restaurant-viewed-' || LPAD(rvw.seq::TEXT, 8, '0'),"
                )
                a("  'ui.restaurant.viewed',")
                a("  'v1',")
                a("  NOW() - ((rvw.seq % 18) || ' days')::INTERVAL - ((rvw.seq % 600) || ' minutes')::INTERVAL,")
                a("  m.id,")
                a("  NULL,")
                a(
                    "  SUBSTR(MD5("
                    + sql_quote(run_token)
                    + " || '-restaurant-view-session-' || m.id::TEXT || '-' || r.id::TEXT), 1, 24),"
                )
                a("  'CLIENT',")
                a("  '/restaurants/' || r.id,")
                a("  'GET',")
                a(
                    "  'device-' || "
                    + sql_quote(run_token)
                    + " || '-slot-' || LPAD(("
                    + sql_bucket_expr(
                        sql_quote(run_token) + " || '-ua-device-' || m.id::TEXT",
                        1,
                        24,
                        1,
                    )
                    + ")::TEXT, 2, '0') || '-' || "
                    + sql_hash_slice_expr(
                        sql_quote(run_token) + " || '-ua-device-' || m.id::TEXT",
                        1,
                        16,
                    )
                    + ","
                )
                a("  cfg.platforms[((rvw.seq - 1) % CARDINALITY(cfg.platforms)) + 1],")
                a("  '2.' || ((rvw.seq % 4) + 1) || '.' || ((rvw.seq % 10) + 1),")
                a("  'ko-KR',")
                a(
                    "  jsonb_build_object("
                    "'restaurantId', r.id, "
                    "'entryPoint', CASE WHEN MOD(rvw.seq, 4) = 0 THEN 'map' WHEN MOD(rvw.seq, 3) = 0 THEN 'favorite' ELSE 'search' END, "
                    "'source', 'CLIENT'"
                    ")"
                )
                a(f"FROM generate_series(1, {effective_user_activity_restaurant_view_count}) AS rvw(seq)")
                a("JOIN tmp_dummy_member m")
                a(f"  ON m.seq = (((rvw.seq - 1) % {member_count}) + 1)")
                a("JOIN tmp_dummy_restaurant r")
                a(f"  ON r.seq = ((((rvw.seq - 1) * 7) % {restaurant_count}) + 1)")
                a("CROSS JOIN cfg;")
                a("")
                ua_offset += effective_user_activity_restaurant_view_count

            if effective_user_activity_page_view_count > 0:
                a("-- User activity: ui.page.viewed")
                a("WITH cfg AS (")
                a(
                    f"  SELECT {sql_text_array(['/home', '/search', '/groups', '/restaurants'])}::TEXT[] AS paths,"
                    f" {sql_text_array(['IOS', 'ANDROID', 'WEB'])}::TEXT[] AS platforms"
                )
                a(")")
                a(
                    "INSERT INTO tmp_async_user_activity_event "
                    "(seq, event_id, event_name, event_version, occurred_at, member_id, anonymous_id, session_id, "
                    "source, request_path, request_method, device_id, platform, app_version, locale, properties)"
                )
                a("SELECT")
                a(f"  {ua_offset} + pv.seq,")
                a(
                    "  'ua-"
                    + run_token
                    + "-page-viewed-' || LPAD(pv.seq::TEXT, 8, '0'),"
                )
                a("  'ui.page.viewed',")
                a("  'v1',")
                a("  NOW() - ((pv.seq % 12) || ' days')::INTERVAL - ((pv.seq % 360) || ' minutes')::INTERVAL,")
                a("  m.id,")
                a("  NULL,")
                a(
                    "  SUBSTR(MD5("
                    + sql_quote(run_token)
                    + " || '-page-view-session-' || m.id::TEXT || '-' || pv.seq::TEXT), 1, 24),"
                )
                a("  'CLIENT',")
                a("  cfg.paths[((pv.seq - 1) % CARDINALITY(cfg.paths)) + 1],")
                a("  'GET',")
                a(
                    "  'device-' || "
                    + sql_quote(run_token)
                    + " || '-slot-' || LPAD(("
                    + sql_bucket_expr(
                        sql_quote(run_token) + " || '-ua-device-' || m.id::TEXT",
                        1,
                        24,
                        1,
                    )
                    + ")::TEXT, 2, '0') || '-' || "
                    + sql_hash_slice_expr(
                        sql_quote(run_token) + " || '-ua-device-' || m.id::TEXT",
                        1,
                        16,
                    )
                    + ","
                )
                a("  cfg.platforms[((pv.seq - 1) % CARDINALITY(cfg.platforms)) + 1],")
                a("  '2.' || ((pv.seq % 4) + 1) || '.' || ((pv.seq % 10) + 1),")
                a("  'ko-KR',")
                a(
                    "  jsonb_build_object("
                    "'page', cfg.paths[((pv.seq - 1) % CARDINALITY(cfg.paths)) + 1], "
                    "'source', 'CLIENT'"
                    ")"
                )
                a(f"FROM generate_series(1, {effective_user_activity_page_view_count}) AS pv(seq)")
                a("JOIN tmp_dummy_member m")
                a(f"  ON m.seq = (((pv.seq - 1) % {member_count}) + 1)")
                a("CROSS JOIN cfg;")
                a("")
                ua_offset += effective_user_activity_page_view_count

            if effective_user_activity_page_dwelled_count > 0:
                a("-- User activity: ui.page.dwelled")
                a("WITH cfg AS (")
                a(
                    f"  SELECT {sql_text_array(['/home', '/search', '/groups', '/restaurants'])}::TEXT[] AS paths,"
                    f" {sql_text_array(['IOS', 'ANDROID', 'WEB'])}::TEXT[] AS platforms"
                )
                a(")")
                a(
                    "INSERT INTO tmp_async_user_activity_event "
                    "(seq, event_id, event_name, event_version, occurred_at, member_id, anonymous_id, session_id, "
                    "source, request_path, request_method, device_id, platform, app_version, locale, properties)"
                )
                a("SELECT")
                a(f"  {ua_offset} + pd.seq,")
                a(
                    "  'ua-"
                    + run_token
                    + "-page-dwelled-' || LPAD(pd.seq::TEXT, 8, '0'),"
                )
                a("  'ui.page.dwelled',")
                a("  'v1',")
                a("  NOW() - ((pd.seq % 10) || ' days')::INTERVAL - ((pd.seq % 240) || ' minutes')::INTERVAL,")
                a("  m.id,")
                a("  NULL,")
                a(
                    "  SUBSTR(MD5("
                    + sql_quote(run_token)
                    + " || '-page-dwelled-session-' || m.id::TEXT || '-' || pd.seq::TEXT), 1, 24),"
                )
                a("  'CLIENT',")
                a("  cfg.paths[((pd.seq - 1) % CARDINALITY(cfg.paths)) + 1],")
                a("  'GET',")
                a(
                    "  'device-' || "
                    + sql_quote(run_token)
                    + " || '-slot-' || LPAD(("
                    + sql_bucket_expr(
                        sql_quote(run_token) + " || '-ua-device-' || m.id::TEXT",
                        1,
                        24,
                        1,
                    )
                    + ")::TEXT, 2, '0') || '-' || "
                    + sql_hash_slice_expr(
                        sql_quote(run_token) + " || '-ua-device-' || m.id::TEXT",
                        1,
                        16,
                    )
                    + ","
                )
                a("  cfg.platforms[((pd.seq - 1) % CARDINALITY(cfg.platforms)) + 1],")
                a("  '2.' || ((pd.seq % 4) + 1) || '.' || ((pd.seq % 10) + 1),")
                a("  'ko-KR',")
                a(
                    "  jsonb_build_object("
                    "'page', cfg.paths[((pd.seq - 1) % CARDINALITY(cfg.paths)) + 1], "
                    "'dwellMillis', 1500 + ((pd.seq % 12) * 700), "
                    "'source', 'CLIENT'"
                    ")"
                )
                a(f"FROM generate_series(1, {effective_user_activity_page_dwelled_count}) AS pd(seq)")
                a("JOIN tmp_dummy_member m")
                a(f"  ON m.seq = (((pd.seq - 1) % {member_count}) + 1)")
                a("CROSS JOIN cfg;")
                a("")

            a(
                "INSERT INTO user_activity_event "
                "(event_id, event_name, event_version, occurred_at, member_id, anonymous_id, session_id, source, "
                "request_path, request_method, device_id, platform, app_version, locale, properties, created_at)"
            )
            a("SELECT")
            a("  event_id,")
            a("  event_name,")
            a("  event_version,")
            a("  occurred_at,")
            a("  member_id,")
            a("  anonymous_id,")
            a("  session_id,")
            a("  source,")
            a("  request_path,")
            a("  request_method,")
            a("  device_id,")
            a("  platform,")
            a("  app_version,")
            a("  locale,")
            a("  properties,")
            a("  NOW()")
            a("FROM tmp_async_user_activity_event")
            a("ON CONFLICT (event_id) DO NOTHING;")
            a("")

            a(
                "CREATE TEMP TABLE tmp_async_source_outbox ("
                "seq INTEGER PRIMARY KEY, event_id VARCHAR(64) NOT NULL UNIQUE, event_name VARCHAR(100) NOT NULL, "
                "event_version VARCHAR(20) NOT NULL, occurred_at TIMESTAMPTZ NOT NULL, member_id BIGINT, "
                "payload JSONB NOT NULL, status VARCHAR(20) NOT NULL, retry_count INTEGER NOT NULL, "
                "next_retry_at TIMESTAMPTZ, last_error VARCHAR(1000), published_at TIMESTAMPTZ, "
                "created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL"
                ") ON COMMIT DROP;"
            )
            a("")
            a(
                "INSERT INTO tmp_async_source_outbox "
                "(seq, event_id, event_name, event_version, occurred_at, member_id, payload, status, retry_count, "
                "next_retry_at, last_error, published_at, created_at, updated_at)"
            )
            a("SELECT")
            a("  seeded.seq,")
            a("  seeded.event_id,")
            a("  seeded.event_name,")
            a("  seeded.event_version,")
            a("  seeded.occurred_at,")
            a("  seeded.member_id,")
            a(
                "  jsonb_build_object("
                "'eventId', seeded.event_id, "
                "'eventName', seeded.event_name, "
                "'eventVersion', seeded.event_version, "
                "'occurredAt', TO_CHAR(seeded.occurred_at AT TIME ZONE 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"'), "
                "'memberId', seeded.member_id, "
                "'anonymousId', seeded.anonymous_id, "
                "'properties', seeded.properties"
                "),"
            )
            a("  seeded.status,")
            a("  seeded.retry_count,")
            a("  CASE WHEN seeded.status = 'PUBLISHED' THEN NULL ELSE NOW() + ((seeded.retry_count * 15) || ' seconds')::INTERVAL END,")
            a(
                "  CASE "
                "    WHEN seeded.status = 'FAILED' THEN '사용자 이벤트 메시지큐 발행 실패가 누적되어 재시도 대기 중입니다.' "
                "    WHEN seeded.status = 'PENDING' THEN '사용자 이벤트 메시지큐 발행 워커 처리 대기 중입니다.' "
                "    ELSE NULL "
                "  END,"
            )
            a("  CASE WHEN seeded.status = 'PUBLISHED' THEN seeded.occurred_at + ((seeded.seq % 20) || ' seconds')::INTERVAL ELSE NULL END,")
            a("  seeded.occurred_at + INTERVAL '1 second',")
            a("  NOW() - ((seeded.seq % 90) || ' minutes')::INTERVAL")
            a("FROM (")
            a("  SELECT")
            a("    e.*,")
            a(
                "    CASE "
                f"      WHEN MOD(e.seq, 100) < {async_source_failed_percent} THEN 'FAILED' "
                f"      WHEN MOD(e.seq, 100) < {async_source_failed_percent + async_source_pending_percent} THEN 'PENDING' "
                "      ELSE 'PUBLISHED' "
                "    END AS status,"
            )
            a(
                "    CASE "
                f"      WHEN MOD(e.seq, 100) < {async_source_failed_percent + async_source_pending_percent} THEN 1 + (e.seq % 4) "
                "      ELSE 0 "
                "    END AS retry_count"
            )
            a("  FROM tmp_async_user_activity_event e")
            a(") seeded;")
            a("")
            a(
                "INSERT INTO user_activity_source_outbox "
                "(event_id, event_name, event_version, occurred_at, member_id, payload, status, retry_count, "
                "next_retry_at, last_error, published_at, created_at, updated_at)"
            )
            a("SELECT")
            a("  event_id, event_name, event_version, occurred_at, member_id, payload, status, retry_count,")
            a("  next_retry_at, last_error, published_at, created_at, updated_at")
            a("FROM tmp_async_source_outbox")
            a("ON CONFLICT (event_id) DO NOTHING;")
            a("")

            a(
                "CREATE TEMP TABLE tmp_async_dispatch_outbox ("
                "seq INTEGER PRIMARY KEY, event_id VARCHAR(64) NOT NULL UNIQUE, dispatch_target VARCHAR(30) NOT NULL, "
                "payload JSONB NOT NULL, status VARCHAR(20) NOT NULL, retry_count INTEGER NOT NULL, "
                "next_retry_at TIMESTAMPTZ, last_error VARCHAR(1000), dispatched_at TIMESTAMPTZ, "
                "created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL"
                ") ON COMMIT DROP;"
            )
            a("")
            a(
                "INSERT INTO tmp_async_dispatch_outbox "
                "(seq, event_id, dispatch_target, payload, status, retry_count, next_retry_at, last_error, "
                "dispatched_at, created_at, updated_at)"
            )
            a("SELECT")
            a("  seeded.seq,")
            a("  seeded.event_id,")
            a("  'POSTHOG',")
            a(
                "  jsonb_build_object("
                "'eventId', seeded.event_id, "
                "'eventName', seeded.event_name, "
                "'eventVersion', seeded.event_version, "
                "'occurredAt', TO_CHAR(seeded.occurred_at AT TIME ZONE 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"'), "
                "'memberId', seeded.member_id, "
                "'anonymousId', seeded.anonymous_id, "
                "'properties', seeded.properties"
                "),"
            )
            a("  seeded.status,")
            a("  seeded.retry_count,")
            a("  CASE WHEN seeded.status = 'DISPATCHED' THEN NULL ELSE NOW() + ((seeded.retry_count * 20) || ' seconds')::INTERVAL END,")
            a(
                "  CASE "
                "    WHEN seeded.status = 'FAILED' THEN 'PostHog 전송 실패로 dispatch outbox 재시도가 필요합니다.' "
                "    WHEN seeded.status = 'PENDING' THEN 'dispatch scheduler 처리 대기 중입니다.' "
                "    ELSE NULL "
                "  END,"
            )
            a("  CASE WHEN seeded.status = 'DISPATCHED' THEN seeded.occurred_at + ((seeded.seq % 40) || ' seconds')::INTERVAL ELSE NULL END,")
            a("  seeded.occurred_at + INTERVAL '2 seconds',")
            a("  NOW() - ((seeded.seq % 60) || ' minutes')::INTERVAL")
            a("FROM (")
            a("  SELECT")
            a("    e.*,")
            a(
                "    CASE "
                f"      WHEN MOD(e.seq, 100) < {async_dispatch_failed_percent} THEN 'FAILED' "
                f"      WHEN MOD(e.seq, 100) < {async_dispatch_failed_percent + async_dispatch_pending_percent} THEN 'PENDING' "
                "      ELSE 'DISPATCHED' "
                "    END AS status,"
            )
            a(
                "    CASE "
                f"      WHEN MOD(e.seq, 100) < {async_dispatch_failed_percent + async_dispatch_pending_percent} THEN 1 + (e.seq % 5) "
                "      ELSE 0 "
                "    END AS retry_count"
            )
            a("  FROM tmp_async_user_activity_event e")
            a(") seeded;")
            a("")
            a(
                "INSERT INTO user_activity_dispatch_outbox "
                "(event_id, dispatch_target, payload, status, retry_count, next_retry_at, last_error, dispatched_at, created_at, updated_at)"
            )
            a("SELECT")
            a("  event_id, dispatch_target, payload, status, retry_count, next_retry_at, last_error, dispatched_at, created_at, updated_at")
            a("FROM tmp_async_dispatch_outbox")
            a("ON CONFLICT (event_id, dispatch_target) DO NOTHING;")
            a("")
            a("-- Message queue trace: user activity pipeline")
            a(
                "INSERT INTO message_queue_trace_log "
                "(message_id, topic, provider, message_key, consumer_group, stage, processing_millis, error_message, created_at)"
            )
            a("SELECT")
            a("  event_id,")
            a("  'domain.user.activity',")
            a("  'redis-stream',")
            a("  COALESCE(member_id::TEXT, event_id),")
            a("  NULL,")
            a("  'PUBLISH',")
            a("  NULL,")
            a("  NULL,")
            a("  COALESCE(published_at, created_at)")
            a("FROM tmp_async_source_outbox")
            a("WHERE status = 'PUBLISHED';")
            a("")
            a(
                "INSERT INTO message_queue_trace_log "
                "(message_id, topic, provider, message_key, consumer_group, stage, processing_millis, error_message, created_at)"
            )
            a("SELECT")
            a("  event_id,")
            a("  'domain.user.activity',")
            a("  'redis-stream',")
            a("  COALESCE(member_id::TEXT, event_id),")
            a("  'tasteam-api-user-activity',")
            a("  CASE WHEN MOD(seq, 67) = 0 THEN 'CONSUME_FAIL' ELSE 'CONSUME_SUCCESS' END,")
            a("  CASE WHEN MOD(seq, 67) = 0 THEN 850 + (seq % 700) ELSE 35 + (seq % 180) END,")
            a(
                "  CASE WHEN MOD(seq, 67) = 0 "
                "    THEN '사용자 이벤트 소비 후 저장 단계에서 재시도 가능한 오류가 발생했습니다.' "
                "    ELSE NULL "
                "  END,"
            )
            a("  COALESCE(published_at, created_at) + INTERVAL '1 second'")
            a("FROM tmp_async_source_outbox")
            a("WHERE status = 'PUBLISHED';")
            a("")

        if effective_notification_outbox_count > 0:
            a(
                "CREATE TEMP TABLE tmp_async_notification_outbox ("
                "seq INTEGER PRIMARY KEY, event_id VARCHAR(64) NOT NULL UNIQUE, event_type VARCHAR(64) NOT NULL, "
                "recipient_id BIGINT NOT NULL, payload JSONB NOT NULL, status VARCHAR(20) NOT NULL, "
                "retry_count INTEGER NOT NULL, next_retry_at TIMESTAMPTZ, last_error VARCHAR(1000), "
                "published_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL"
                ") ON COMMIT DROP;"
            )
            a("")
            a(
                "INSERT INTO tmp_async_notification_outbox "
                "(seq, event_id, event_type, recipient_id, payload, status, retry_count, next_retry_at, last_error, "
                "published_at, created_at, updated_at)"
            )
            a("SELECT")
            a("  seeded.rn,")
            a("  seeded.event_id,")
            a(
                "  CASE MOD(seeded.rn, 4) "
                "    WHEN 0 THEN 'GroupMemberJoinedEvent' "
                "    WHEN 1 THEN 'GroupRequestSubmittedEvent' "
                "    WHEN 2 THEN 'GroupRequestReviewedEvent' "
                "    ELSE 'MemberRegisteredEvent' "
                "  END,"
            )
            a("  seeded.recipient_id,")
            a(
                "  jsonb_build_object("
                "'eventId', seeded.event_id, "
                "'eventType', CASE MOD(seeded.rn, 4) "
                "  WHEN 0 THEN 'GroupMemberJoinedEvent' "
                "  WHEN 1 THEN 'GroupRequestSubmittedEvent' "
                "  WHEN 2 THEN 'GroupRequestReviewedEvent' "
                "  ELSE 'MemberRegisteredEvent' "
                "END, "
                "'recipientId', seeded.recipient_id, "
                "'notificationType', seeded.notification_type, "
                "'channels', CASE seeded.notification_type "
                "  WHEN 'NOTICE' THEN jsonb_build_array('WEB', 'PUSH', 'EMAIL') "
                "  WHEN 'CHAT' THEN jsonb_build_array('WEB', 'PUSH') "
                "  ELSE jsonb_build_array('WEB', 'PUSH') "
                "END, "
                "'templateKey', CASE MOD(seeded.rn, 4) "
                "  WHEN 0 THEN 'group-joined' "
                "  WHEN 1 THEN 'group-request-submitted' "
                "  WHEN 2 THEN 'group-request-reviewed' "
                "  ELSE 'member-welcome' "
                "END, "
                "'templateVariables', jsonb_build_object('title', seeded.title, 'body', seeded.body), "
                "'deepLink', seeded.deep_link, "
                "'occurredAt', TO_CHAR(seeded.occurred_at AT TIME ZONE 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"')"
                "),"
            )
            a("  seeded.status,")
            a("  seeded.retry_count,")
            a("  CASE WHEN seeded.status = 'PUBLISHED' THEN NULL ELSE NOW() + ((seeded.retry_count * 30) || ' seconds')::INTERVAL END,")
            a(
                "  CASE "
                "    WHEN seeded.status = 'FAILED' THEN '알림 아웃박스 메시지큐 발행 실패가 누적되었습니다.' "
                "    WHEN seeded.status = 'PENDING' THEN '알림 아웃박스 스캐너의 다음 배치를 기다리고 있습니다.' "
                "    ELSE NULL "
                "  END,"
            )
            a("  CASE WHEN seeded.status = 'PUBLISHED' THEN seeded.occurred_at + ((seeded.rn % 15) || ' seconds')::INTERVAL ELSE NULL END,")
            a("  seeded.occurred_at + INTERVAL '1 second',")
            a("  NOW() - ((seeded.rn % 45) || ' minutes')::INTERVAL")
            a("FROM (")
            a("  SELECT")
            a("    n.event_id,")
            a("    n.member_id AS recipient_id,")
            a("    n.notification_type,")
            a("    n.title,")
            a("    n.body,")
            a("    n.deep_link,")
            a("    n.created_at AS occurred_at,")
            a("    ROW_NUMBER() OVER (ORDER BY n.id) AS rn,")
            a(
                "    CASE "
                f"      WHEN MOD(ROW_NUMBER() OVER (ORDER BY n.id), 100) < {async_notification_failed_percent} THEN 'FAILED' "
                f"      WHEN MOD(ROW_NUMBER() OVER (ORDER BY n.id), 100) < {async_notification_failed_percent + async_notification_pending_percent} THEN 'PENDING' "
                "      ELSE 'PUBLISHED' "
                "    END AS status,"
            )
            a(
                "    CASE "
                f"      WHEN MOD(ROW_NUMBER() OVER (ORDER BY n.id), 100) < {async_notification_failed_percent + async_notification_pending_percent} "
                "      THEN 1 + (ROW_NUMBER() OVER (ORDER BY n.id) % 4) "
                "      ELSE 0 "
                "    END AS retry_count"
            )
            a("  FROM notification n")
            a(
                "  WHERE n.event_id LIKE "
                + sql_quote(content["notification_event_prefix"] + "-" + run_token + "-%")
            )
            a(") seeded")
            a(f"WHERE seeded.rn <= {effective_notification_outbox_count};")
            a("")
            a(
                "INSERT INTO notification_outbox "
                "(event_id, event_type, recipient_id, payload, status, retry_count, next_retry_at, last_error, published_at, created_at, updated_at)"
            )
            a("SELECT")
            a("  event_id, event_type, recipient_id, payload, status, retry_count, next_retry_at, last_error, published_at, created_at, updated_at")
            a("FROM tmp_async_notification_outbox")
            a("ON CONFLICT (event_id) DO NOTHING;")
            a("")
            a("INSERT INTO consumed_notification_event (consumer_group, event_id, stream_key, processed_at)")
            a("SELECT")
            a("  'cg.notification.processor.v1',")
            a("  event_id,")
            a("  'evt.notification.v1',")
            a("  COALESCE(published_at, created_at) + INTERVAL '2 seconds'")
            a("FROM tmp_async_notification_outbox")
            a("WHERE status = 'PUBLISHED'")
            a("  AND MOD(seq, 53) <> 0")
            a("ON CONFLICT (consumer_group, event_id) DO NOTHING;")
            a("")
            a("-- Message queue trace: notification pipeline")
            a(
                "INSERT INTO message_queue_trace_log "
                "(message_id, topic, provider, message_key, consumer_group, stage, processing_millis, error_message, created_at)"
            )
            a("SELECT")
            a("  event_id,")
            a("  'evt.notification.v1',")
            a("  'redis-stream',")
            a("  recipient_id::TEXT,")
            a("  NULL,")
            a("  'PUBLISH',")
            a("  NULL,")
            a("  NULL,")
            a("  COALESCE(published_at, created_at)")
            a("FROM tmp_async_notification_outbox")
            a("WHERE status = 'PUBLISHED';")
            a("")
            a(
                "INSERT INTO message_queue_trace_log "
                "(message_id, topic, provider, message_key, consumer_group, stage, processing_millis, error_message, created_at)"
            )
            a("SELECT")
            a("  event_id,")
            a("  'evt.notification.v1',")
            a("  'redis-stream',")
            a("  recipient_id::TEXT,")
            a("  'cg.notification.processor.v1',")
            a("  CASE WHEN MOD(seq, 53) = 0 THEN 'CONSUME_FAIL' ELSE 'CONSUME_SUCCESS' END,")
            a("  CASE WHEN MOD(seq, 53) = 0 THEN 950 + (seq % 500) ELSE 45 + (seq % 140) END,")
            a(
                "  CASE WHEN MOD(seq, 53) = 0 "
                "    THEN '알림 메시지 역직렬화 또는 처리 단계에서 예외가 발생해 DLQ 전송 후보가 되었습니다.' "
                "    ELSE NULL "
                "  END,"
            )
            a("  COALESCE(published_at, created_at) + INTERVAL '1 second'")
            a("FROM tmp_async_notification_outbox")
            a("WHERE status = 'PUBLISHED';")
            a("")

        if effective_user_activity_group_join_count > 0:
            a("-- Message queue trace: group member joined notifications")
            a(
                "INSERT INTO message_queue_trace_log "
                "(message_id, topic, provider, message_key, consumer_group, stage, processing_millis, error_message, created_at)"
            )
            a("SELECT")
            a(
                "  'mq-group-joined-"
                + run_token
                + "-' || LPAD(seeded.rn::TEXT, 8, '0'),"
            )
            a("  'domain.group.member-joined',")
            a("  'redis-stream',")
            a("  seeded.member_id::TEXT,")
            a("  NULL,")
            a("  'PUBLISH',")
            a("  NULL,")
            a("  NULL,")
            a("  NOW() - ((seeded.rn % 21) || ' days')::INTERVAL - ((seeded.rn % 360) || ' minutes')::INTERVAL")
            a("FROM (")
            a("  SELECT gm.member_id, ROW_NUMBER() OVER (ORDER BY gm.id) AS rn")
            a("  FROM group_member gm")
            a("  JOIN tmp_dummy_group tg ON tg.id = gm.group_id")
            a(") seeded")
            a(f"WHERE seeded.rn <= {effective_user_activity_group_join_count};")
            a("")
            a(
                "INSERT INTO message_queue_trace_log "
                "(message_id, topic, provider, message_key, consumer_group, stage, processing_millis, error_message, created_at)"
            )
            a("SELECT")
            a(
                "  'mq-group-joined-"
                + run_token
                + "-' || LPAD(seeded.rn::TEXT, 8, '0'),"
            )
            a("  'domain.group.member-joined',")
            a("  'redis-stream',")
            a("  seeded.member_id::TEXT,")
            a("  'tasteam-api',")
            a("  CASE WHEN MOD(seeded.rn, 71) = 0 THEN 'CONSUME_FAIL' ELSE 'CONSUME_SUCCESS' END,")
            a("  CASE WHEN MOD(seeded.rn, 71) = 0 THEN 700 + (seeded.rn % 300) ELSE 25 + (seeded.rn % 120) END,")
            a(
                "  CASE WHEN MOD(seeded.rn, 71) = 0 "
                "    THEN '그룹 가입 메시지 후속 알림 생성 중 일시 오류가 발생했습니다.' "
                "    ELSE NULL "
                "  END,"
            )
            a("  NOW() - ((seeded.rn % 21) || ' days')::INTERVAL - ((seeded.rn % 360) || ' minutes')::INTERVAL + INTERVAL '1 second'")
            a("FROM (")
            a("  SELECT gm.member_id, ROW_NUMBER() OVER (ORDER BY gm.id) AS rn")
            a("  FROM group_member gm")
            a("  JOIN tmp_dummy_group tg ON tg.id = gm.group_id")
            a(") seeded")
            a(f"WHERE seeded.rn <= {effective_user_activity_group_join_count};")
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
    a("DELETE FROM group_auth_code WHERE group_id = 2002;")
    a(
        "INSERT INTO group_auth_code (id, group_id, code, created_at) "
        f"VALUES (nextval('group_auth_code_seq'), 2002, {group_join_code_hash_sql}, NOW());"
    )
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
    user_activity_event_like = "ua-" + run_token.replace("%", "\\%") + "-%"
    group_join_trace_like = "mq-group-joined-" + run_token.replace("%", "\\%") + "-%"
    asset_token_like = "%/" + run_token.replace("%", "\\%") + "/%"

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
    a(f"SELECT id FROM \"group\" WHERE logo_image_url LIKE {sql_quote(asset_token_like)};")
    a("INSERT INTO tmp_cleanup_group_ids (id)")
    a(
        "SELECT DISTINCT group_id "
        "FROM subgroup "
        f"WHERE profile_image_url LIKE {sql_quote(asset_token_like)};"
    )
    a("INSERT INTO tmp_cleanup_group_ids (id)")
    a("SELECT 2002 WHERE EXISTS (SELECT 1 FROM \"group\" WHERE id = 2002);")
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_restaurant_ids AS")
    a(f"SELECT id FROM restaurant WHERE name LIKE {sql_quote(restaurant_like)};")
    a("INSERT INTO tmp_cleanup_restaurant_ids (id)")
    a(
        "SELECT DISTINCT mc.restaurant_id "
        "FROM menu_category mc "
        "JOIN menu m ON m.category_id = mc.id "
        f"WHERE m.image_url LIKE {sql_quote(asset_token_like)};"
    )
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_subgroup_ids AS")
    a("SELECT id FROM subgroup WHERE group_id IN (SELECT id FROM tmp_cleanup_group_ids);")
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_chat_room_ids AS")
    a("SELECT id FROM chat_room WHERE subgroup_id IN (SELECT id FROM tmp_cleanup_subgroup_ids);")
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_review_ids AS")
    a("SELECT id FROM review WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids)")
    a("   OR restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);")
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_announcement_ids AS")
    a(
        "SELECT id FROM announcement "
        f"WHERE title LIKE {sql_quote('시드공지-' + run_token + '-%')};"
    )
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_promotion_ids AS")
    a("SELECT id FROM promotion")
    a(
        f"WHERE title LIKE {sql_quote('시드프로모션-' + run_token + '-%')}"
        f"   OR landing_url LIKE {sql_quote('%/' + run_token + '/%')};"
    )
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_report_ids AS")
    a("SELECT id FROM report")
    a(
        f"WHERE content LIKE {sql_quote('시드신고-' + run_token + '-%')}"
        "   OR member_id IN (SELECT id FROM tmp_cleanup_member_ids);"
    )
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_image_ids AS")
    a("SELECT id FROM image")
    a(f"WHERE storage_key LIKE {sql_quote(asset_token_like)};")
    a("")
    a("CREATE TEMP TABLE tmp_cleanup_domain_image_ids AS")
    a("SELECT id FROM domain_image WHERE image_id IN (SELECT id FROM tmp_cleanup_image_ids);")
    a("")

    a("DO $$")
    a("BEGIN")
    a("  IF to_regclass('public.image_optimization_job') IS NOT NULL THEN")
    a("    DELETE FROM image_optimization_job WHERE image_id IN (SELECT id FROM tmp_cleanup_image_ids);")
    a("  END IF;")
    a("END $$;")
    a("")
    a(
        "DELETE FROM chat_message_file "
        "WHERE chat_message_id IN ("
        "  SELECT id FROM chat_message WHERE chat_room_id IN (SELECT id FROM tmp_cleanup_chat_room_ids)"
        ") "
        "   OR domain_image_id IN (SELECT id FROM tmp_cleanup_domain_image_ids);"
    )
    a("DO $$")
    a("BEGIN")
    a("  IF to_regclass('public.review_image') IS NOT NULL THEN")
    a(
        "    DELETE FROM review_image "
        "WHERE review_id IN (SELECT id FROM tmp_cleanup_review_ids) "
        f"   OR image_url LIKE {sql_quote(asset_token_like)};"
    )
    a("  END IF;")
    a("END $$;")
    a("")
    a("DO $$")
    a("BEGIN")
    a("  IF to_regclass('public.restaurant_image') IS NOT NULL THEN")
    a(
        "    DELETE FROM restaurant_image "
        "WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids) "
        f"   OR image_url LIKE {sql_quote(asset_token_like)};"
    )
    a("  END IF;")
    a("END $$;")
    a("")
    a("DO $$")
    a("BEGIN")
    a("  IF to_regclass('public.restaurant_ai_results') IS NOT NULL THEN")
    a(
        "    DELETE FROM restaurant_ai_results "
        "WHERE restaurant_id::BIGINT IN (SELECT id FROM tmp_cleanup_restaurant_ids) "
        f"   OR restaurant_name LIKE {sql_quote(restaurant_like)};"
    )
    a("  END IF;")
    a("END $$;")
    a("")
    a("DO $$")
    a("BEGIN")
    a("  IF to_regclass('public.ai_restaurant_feature') IS NOT NULL THEN")
    a("    DELETE FROM ai_restaurant_feature WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);")
    a("  END IF;")
    a("END $$;")
    a("")
    a("DELETE FROM promotion_asset WHERE promotion_id IN (SELECT id FROM tmp_cleanup_promotion_ids)")
    a(f"   OR image_url LIKE {sql_quote(asset_token_like)};")
    a("DELETE FROM promotion_display WHERE promotion_id IN (SELECT id FROM tmp_cleanup_promotion_ids);")
    a("DELETE FROM promotion WHERE id IN (SELECT id FROM tmp_cleanup_promotion_ids);")
    a("DELETE FROM announcement WHERE id IN (SELECT id FROM tmp_cleanup_announcement_ids);")
    a("DELETE FROM report WHERE id IN (SELECT id FROM tmp_cleanup_report_ids);")
    a(
        "DELETE FROM push_notification_target "
        "WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids) "
        f"   OR device_id LIKE {sql_quote('device-' + run_token + '-%')} "
        f"   OR fcm_token LIKE {sql_quote('fcm-' + run_token + '-%')};"
    )
    a("DELETE FROM refresh_token WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("DELETE FROM member_notification_preference WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("DELETE FROM member_search_history WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("DELETE FROM member_favorite_restaurant WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a(
        "DELETE FROM message_queue_trace_log "
        f"WHERE message_id LIKE {sql_quote(user_activity_event_like)} "
        f"   OR message_id LIKE {sql_quote(event_like)} "
        f"   OR message_id LIKE {sql_quote(group_join_trace_like)};"
    )
    a(
        "DELETE FROM consumed_notification_event "
        "WHERE consumer_group = 'cg.notification.processor.v1' "
        f"  AND event_id LIKE {sql_quote(event_like)};"
    )
    a(f"DELETE FROM notification_outbox WHERE event_id LIKE {sql_quote(event_like)};")
    a("DELETE FROM user_activity_dispatch_outbox")
    a(f"WHERE event_id LIKE {sql_quote(user_activity_event_like)};")
    a("DELETE FROM user_activity_source_outbox")
    a(f"WHERE event_id LIKE {sql_quote(user_activity_event_like)};")
    a(f"DELETE FROM user_activity_event WHERE event_id LIKE {sql_quote(user_activity_event_like)};")
    a(
        "DELETE FROM subgroup_favorite_restaurant "
        "WHERE subgroup_id IN (SELECT id FROM tmp_cleanup_subgroup_ids) "
        "   OR member_id IN (SELECT id FROM tmp_cleanup_member_ids);"
    )
    a(f"DELETE FROM notification WHERE event_id LIKE {sql_quote(event_like)};")
    a("DELETE FROM notification WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);")
    a("")
    a("DELETE FROM review_keyword WHERE review_id IN (SELECT id FROM tmp_cleanup_review_ids);")
    a("DELETE FROM review WHERE id IN (SELECT id FROM tmp_cleanup_review_ids);")
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
    a("DELETE FROM restaurant_schedule_override WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);")
    a("DELETE FROM restaurant_comparison WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);")
    a(
        "DELETE FROM restaurant_review_summary WHERE restaurant_id IN "
        "(SELECT id FROM tmp_cleanup_restaurant_ids);"
    )
    a(
        "DELETE FROM restaurant_review_sentiment WHERE restaurant_id IN "
        "(SELECT id FROM tmp_cleanup_restaurant_ids);"
    )
    a(
        "DELETE FROM domain_image "
        "WHERE id IN (SELECT id FROM tmp_cleanup_domain_image_ids) "
        "   OR image_id IN (SELECT id FROM tmp_cleanup_image_ids);"
    )
    a("DELETE FROM image WHERE id IN (SELECT id FROM tmp_cleanup_image_ids);")
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
        default=SCRIPT_DIR / "default_seed_profile.json",
        help="JSON 설정 파일 경로 (미지정 시 default_seed_profile.json 사용)",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=SCRIPT_DIR.parent / "results" / "generated-seed" / "latest" / "seed.sql",
        help="생성할 seed SQL 파일 경로",
    )
    parser.add_argument(
        "--cleanup-output",
        type=Path,
        default=SCRIPT_DIR.parent / "results" / "generated-seed" / "latest" / "cleanup.sql",
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
