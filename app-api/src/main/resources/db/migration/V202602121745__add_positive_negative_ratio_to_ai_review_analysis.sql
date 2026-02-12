-- Guard against environments where the table/columns may already exist.
ALTER TABLE IF EXISTS ai_restaurant_review_analysis
	ADD COLUMN IF NOT EXISTS positive_ratio numeric(5, 4) NOT NULL DEFAULT 0.0000,
	ADD COLUMN IF NOT EXISTS negative_ratio numeric(5, 4) NOT NULL DEFAULT 0.0000;
