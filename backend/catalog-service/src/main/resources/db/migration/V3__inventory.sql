-- Module inventory: tồn kho theo SKU và giữ hàng (reservation) có hạn.
-- available = on_hand - reserved; cập nhật bằng optimistic locking (version), không khóa bi quan.

CREATE TABLE inventory_item
(
    id                  UUID PRIMARY KEY,
    sku_id              UUID                     NOT NULL UNIQUE REFERENCES product_variant (id),
    on_hand             INT                      NOT NULL DEFAULT 0 CHECK (on_hand >= 0),
    reserved            INT                      NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    low_stock_threshold INT                      NOT NULL DEFAULT 3,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(64)              NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by          VARCHAR(64)              NOT NULL,
    version             BIGINT                   NOT NULL DEFAULT 0
);

CREATE TABLE reservation
(
    id         UUID PRIMARY KEY,
    order_id   UUID                     NOT NULL,
    sku_id     UUID                     NOT NULL REFERENCES product_variant (id),
    quantity   INT                      NOT NULL CHECK (quantity > 0),
    status     VARCHAR(20)              NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64)              NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64)              NOT NULL,
    version    BIGINT                   NOT NULL DEFAULT 0,
    UNIQUE (order_id, sku_id)
);
CREATE INDEX ix_reservation_order ON reservation (order_id);
CREATE INDEX ix_reservation_expiry ON reservation (expires_at) WHERE status = 'RESERVED';

-- Lịch sử thay đổi tồn kho (ai nhập/xuất, lúc nào) qua Envers
CREATE TABLE inventory_item_aud
(
    id                  UUID   NOT NULL,
    rev                 BIGINT NOT NULL REFERENCES revinfo (id),
    revtype             SMALLINT,
    sku_id              UUID,
    on_hand             INT,
    reserved            INT,
    low_stock_threshold INT,
    PRIMARY KEY (id, rev)
);
