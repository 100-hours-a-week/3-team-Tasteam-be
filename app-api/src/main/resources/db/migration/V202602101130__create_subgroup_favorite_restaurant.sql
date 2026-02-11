CREATE TABLE IF NOT EXISTS subgroup_favorite_restaurant (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    subgroup_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_subgroup_favorite_subgroup_restaurant UNIQUE (subgroup_id, restaurant_id),
    CONSTRAINT uq_subgroup_favorite_restaurant_member UNIQUE (restaurant_id, member_id),
    CONSTRAINT fk_subgroup_favorite_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_subgroup_favorite_subgroup FOREIGN KEY (subgroup_id) REFERENCES subgroup (id),
    CONSTRAINT fk_subgroup_favorite_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurant (id)
);

CREATE INDEX IF NOT EXISTS idx_subgroup_favorite_subgroup_created_at
    ON subgroup_favorite_restaurant (subgroup_id, created_at DESC);
