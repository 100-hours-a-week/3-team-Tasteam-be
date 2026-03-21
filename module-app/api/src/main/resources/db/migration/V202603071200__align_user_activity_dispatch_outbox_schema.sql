-- user_activity_dispatch_outbox 스키마를 코드 계약(dispatch_target, DISPATCHED)과 정렬
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'user_activity_dispatch_outbox'
          AND column_name = 'sink_type'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'user_activity_dispatch_outbox'
          AND column_name = 'dispatch_target'
    ) THEN
        ALTER TABLE public.user_activity_dispatch_outbox
            RENAME COLUMN sink_type TO dispatch_target;
    END IF;
END
$$;

UPDATE public.user_activity_dispatch_outbox
SET status = 'DISPATCHED'
WHERE status = 'SENT';
