ALTER TABLE image
    DROP CONSTRAINT IF EXISTS image_purpose_check;

ALTER TABLE image
    ADD CONSTRAINT image_purpose_check
    CHECK (purpose IN (
        'REVIEW_IMAGE',
        'RESTAURANT_IMAGE',
        'MENU_IMAGE',
        'COMMON_ASSET',
        'PROFILE_IMAGE',
        'GROUP_IMAGE',
        'CHAT_IMAGE'
    ));
