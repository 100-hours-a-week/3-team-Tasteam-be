-- 요약/감정은 restaurant_review_summary, restaurant_review_sentiment로 통일됨. 구 테이블 제거.
DROP TABLE IF EXISTS ai_restaurant_review_analysis;
DROP SEQUENCE IF EXISTS ai_restaurant_review_analysis_id_seq;
