-- 감정 비율 컬럼명을 ratio → percent 로 변경 (0–100 퍼센트 저장)
ALTER TABLE restaurant_review_sentiment
    RENAME COLUMN positive_ratio TO positive_percent;
ALTER TABLE restaurant_review_sentiment
    RENAME COLUMN negative_ratio TO negative_percent;
ALTER TABLE restaurant_review_sentiment
    RENAME COLUMN neutral_ratio TO neutral_percent;
