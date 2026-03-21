-- 요약/감정을 이력 쌓기 → 갱신 방식(음식점당 1행)으로 전환

-- 기존 (restaurant_id, vector_epoch) 유니크 제거 후 restaurant_id 단일 유니크로 변경
DROP INDEX IF EXISTS uq_restaurant_review_sentiment_restaurant_epoch;
CREATE UNIQUE INDEX IF NOT EXISTS uq_restaurant_review_sentiment_restaurant
  ON restaurant_review_sentiment(restaurant_id);

DROP INDEX IF EXISTS uq_restaurant_review_summary_restaurant_epoch;
CREATE UNIQUE INDEX IF NOT EXISTS uq_restaurant_review_summary_restaurant
  ON restaurant_review_summary(restaurant_id);
