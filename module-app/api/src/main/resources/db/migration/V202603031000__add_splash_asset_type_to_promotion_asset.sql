ALTER TABLE promotion_asset
	DROP CONSTRAINT IF EXISTS promotion_asset_asset_type_check;

ALTER TABLE promotion_asset
	ADD CONSTRAINT promotion_asset_asset_type_check
		CHECK ((asset_type)::text = ANY
			((ARRAY ['BANNER'::character varying, 'SPLASH'::character varying, 'DETAIL'::character varying])::text[]));

INSERT INTO promotion_asset (
	is_primary,
	sort_order,
	created_at,
	deleted_at,
	promotion_id,
	updated_at,
	asset_type,
	alt_text,
	image_url
)
SELECT
	true,
	0,
	now(),
	NULL,
	b.promotion_id,
	now(),
	'SPLASH',
	b.alt_text,
	b.image_url
FROM promotion_asset b
WHERE b.asset_type = 'BANNER'
  AND b.is_primary = true
  AND b.deleted_at IS NULL
  AND NOT EXISTS (
	SELECT 1
	FROM promotion_asset s
	WHERE s.promotion_id = b.promotion_id
	  AND s.asset_type = 'SPLASH'
	  AND s.deleted_at IS NULL
);
