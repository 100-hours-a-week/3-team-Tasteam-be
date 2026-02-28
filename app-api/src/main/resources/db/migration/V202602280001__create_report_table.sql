DO $$
BEGIN
    IF to_regclass('public.member') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS report (
            id         BIGSERIAL PRIMARY KEY,
            member_id  BIGINT NOT NULL,
            category   VARCHAR(50) NOT NULL,
            content    TEXT,
            status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
            created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            CONSTRAINT fk_report_member FOREIGN KEY (member_id) REFERENCES member(id)
        );
        CREATE INDEX IF NOT EXISTS idx_report_member_id ON report(member_id, id DESC);
        CREATE INDEX IF NOT EXISTS idx_report_status    ON report(status, id DESC);
        CREATE INDEX IF NOT EXISTS idx_report_category  ON report(category, id DESC);
    END IF;
END $$;
