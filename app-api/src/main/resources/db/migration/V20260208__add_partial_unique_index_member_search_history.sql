DROP INDEX IF EXISTS idx_member_search_history_member_keyword;

WITH duplicates AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY member_id, keyword
               ORDER BY count DESC, updated_at DESC, id
           ) AS rn,
           SUM(count) OVER (PARTITION BY member_id, keyword) AS total_count
    FROM member_search_history
    WHERE deleted_at IS NULL
),
to_keep AS (
    SELECT id, total_count
    FROM duplicates
    WHERE rn = 1
)
UPDATE member_search_history h
SET count = tk.total_count
FROM to_keep tk
WHERE h.id = tk.id AND h.count != tk.total_count;

WITH duplicates AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY member_id, keyword
               ORDER BY count DESC, updated_at DESC, id
           ) AS rn
    FROM member_search_history
    WHERE deleted_at IS NULL
)
DELETE FROM member_search_history
WHERE id IN (SELECT id FROM duplicates WHERE rn > 1);

CREATE UNIQUE INDEX idx_member_search_history_member_keyword_active
    ON member_search_history (member_id, keyword)
    WHERE deleted_at IS NULL;
