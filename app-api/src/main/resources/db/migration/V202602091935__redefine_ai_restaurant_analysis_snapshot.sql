-- Review summary + sentiment snapshot
DO $$
BEGIN
    IF to_regclass('public.ai_restaurant_review_analysis') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'ai_restaurant_review_analysis'
              AND column_name = 'summary'
        ) THEN
            ALTER TABLE ai_restaurant_review_analysis
                RENAME COLUMN summary TO overall_summary;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'ai_restaurant_review_analysis'
              AND column_name = 'positive_review_ratio'
        ) THEN
            ALTER TABLE ai_restaurant_review_analysis
                RENAME COLUMN positive_review_ratio TO positive_ratio;
        END IF;

        ALTER TABLE ai_restaurant_review_analysis
            ALTER COLUMN positive_ratio TYPE NUMERIC(5, 4) USING positive_ratio::NUMERIC(5, 4);

        ALTER TABLE ai_restaurant_review_analysis
            ADD COLUMN IF NOT EXISTS category_summaries JSONB NOT NULL DEFAULT '{}'::jsonb,
            ADD COLUMN IF NOT EXISTS negative_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS analyzed_at TIMESTAMPTZ,
            ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ANALYZING';

        UPDATE ai_restaurant_review_analysis
        SET negative_ratio = GREATEST(0, 1 - positive_ratio),
            analyzed_at = COALESCE(analyzed_at, updated_at),
            status = 'COMPLETED'
        WHERE status = 'ANALYZING' OR analyzed_at IS NULL;
    END IF;
END $$;

-- Restaurant comparison snapshot (AiRestaurantFeature -> AiRestaurantComparison)
DO $$
BEGIN
    IF to_regclass('public.ai_restaurant_feature') IS NOT NULL
       AND to_regclass('public.ai_restaurant_comparison') IS NULL THEN
        ALTER TABLE ai_restaurant_feature
            RENAME TO ai_restaurant_comparison;
    END IF;

    IF to_regclass('public.ai_restaurant_comparison') IS NOT NULL THEN
        ALTER TABLE ai_restaurant_comparison
            ADD COLUMN IF NOT EXISTS category_lift JSONB NOT NULL DEFAULT '{}'::jsonb,
            ADD COLUMN IF NOT EXISTS comparison_display JSONB NOT NULL DEFAULT '[]'::jsonb,
            ADD COLUMN IF NOT EXISTS total_candidates INTEGER NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS validated_count INTEGER NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS analyzed_at TIMESTAMPTZ,
            ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ANALYZING';

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'ai_restaurant_comparison'
              AND column_name = 'content'
        ) THEN
            UPDATE ai_restaurant_comparison
            SET comparison_display = CASE
                    WHEN content IS NULL OR BTRIM(content) = '' THEN '[]'::jsonb
                    ELSE jsonb_build_array(content)
                END
            WHERE comparison_display = '[]'::jsonb;
        END IF;

        UPDATE ai_restaurant_comparison
        SET analyzed_at = COALESCE(analyzed_at, updated_at),
            status = 'COMPLETED'
        WHERE analyzed_at IS NULL OR status = 'ANALYZING';

        ALTER TABLE ai_restaurant_comparison
            DROP COLUMN IF EXISTS content;
    END IF;
END $$;

-- Vector recommendation cache (TTL-like persistence)
DO $$
BEGIN
    IF to_regclass('public.ai_restaurant_recommendation') IS NOT NULL THEN
        ALTER TABLE ai_restaurant_recommendation
            ADD COLUMN IF NOT EXISTS cache_key VARCHAR(120),
            ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;

        UPDATE ai_restaurant_recommendation
        SET cache_key = COALESCE(cache_key, CONCAT('legacy-', id)),
            expires_at = COALESCE(expires_at, created_at + INTERVAL '1 day');

        ALTER TABLE ai_restaurant_recommendation
            ALTER COLUMN cache_key SET NOT NULL,
            ALTER COLUMN expires_at SET NOT NULL;

        CREATE INDEX IF NOT EXISTS idx_ai_restaurant_recommendation_cache_key
            ON ai_restaurant_recommendation (cache_key);

        CREATE INDEX IF NOT EXISTS idx_ai_restaurant_recommendation_expires_at
            ON ai_restaurant_recommendation (expires_at);
    END IF;
END $$;
