ALTER TABLE image
    DROP CONSTRAINT IF EXISTS chk_image_purpose;

ALTER TABLE image
    ADD CONSTRAINT chk_image_purpose
    CHECK (purpose IN (
        'REVIEW_IMAGE',
        'RESTAURANT_IMAGE',
        'MENU_IMAGE',
        'COMMON_ASSET',
        'PROFILE_IMAGE',
        'GROUP_IMAGE',
        'CHAT_IMAGE'
    ));
