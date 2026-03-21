DO $$
BEGIN
    CREATE TABLE IF NOT EXISTS image (
        id BIGINT PRIMARY KEY,
        file_name VARCHAR(256) NOT NULL,
        file_size BIGINT NOT NULL,
        file_type VARCHAR(64) NOT NULL,
        storage_key VARCHAR(512) NOT NULL,
        file_uuid UUID NOT NULL,
        status VARCHAR(16) NOT NULL,
        purpose VARCHAR(32) NOT NULL,
        deleted_at TIMESTAMPTZ,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE SEQUENCE IF NOT EXISTS image_seq START WITH 1 INCREMENT BY 50;

    ALTER TABLE image
        ALTER COLUMN id SET DEFAULT nextval('image_seq');

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_image_storage_key'
    ) THEN
        ALTER TABLE image
            ADD CONSTRAINT uq_image_storage_key UNIQUE (storage_key);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_image_file_uuid'
    ) THEN
        ALTER TABLE image
            ADD CONSTRAINT uq_image_file_uuid UNIQUE (file_uuid);
    END IF;

    CREATE TABLE IF NOT EXISTS domain_image (
        id BIGINT PRIMARY KEY,
        domain_type VARCHAR(32) NOT NULL,
        domain_id BIGINT NOT NULL,
        image_id BIGINT NOT NULL,
        sort_order INTEGER,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE SEQUENCE IF NOT EXISTS domain_image_seq START WITH 1 INCREMENT BY 50;

    ALTER TABLE domain_image
        ALTER COLUMN id SET DEFAULT nextval('domain_image_seq');

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_domain_image_link'
    ) THEN
        ALTER TABLE domain_image
            ADD CONSTRAINT uq_domain_image_link UNIQUE (domain_type, domain_id, image_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_domain_image_image'
    ) THEN
        ALTER TABLE domain_image
            ADD CONSTRAINT fk_domain_image_image FOREIGN KEY (image_id) REFERENCES image(id);
    END IF;

    CREATE INDEX IF NOT EXISTS idx_domain_image_domain
        ON domain_image (domain_type, domain_id, sort_order);
END $$;
