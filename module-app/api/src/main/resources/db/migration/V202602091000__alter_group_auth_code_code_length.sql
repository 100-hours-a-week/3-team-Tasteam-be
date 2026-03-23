DO $$
BEGIN
    IF to_regclass('public.group_auth_code') IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_name = 'group_auth_code'
             AND column_name = 'code'
       ) THEN
        ALTER TABLE group_auth_code
            ALTER COLUMN code TYPE VARCHAR(255);
    END IF;
END $$;
