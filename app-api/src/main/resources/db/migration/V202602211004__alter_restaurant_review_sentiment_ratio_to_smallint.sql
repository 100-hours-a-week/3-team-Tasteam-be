-- 감정 비율을 0~1 → 0~100 퍼센트(SMALLINT)로 저장
ALTER TABLE restaurant_review_sentiment
    ALTER COLUMN positive_ratio TYPE SMALLINT USING (ROUND(positive_ratio * 100)::SMALLINT),
    ALTER COLUMN negative_ratio TYPE SMALLINT USING (ROUND(negative_ratio * 100)::SMALLINT),
    ALTER COLUMN neutral_ratio TYPE SMALLINT USING (ROUND(neutral_ratio * 100)::SMALLINT);
