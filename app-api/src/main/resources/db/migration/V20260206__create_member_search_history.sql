CREATE TABLE member_search_history (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    count BIGINT NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE member_search_history IS '회원 검색어 기록';
