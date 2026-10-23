-- Module flashsale (E7.2, B-30): giá giảm có thời hạn + số suất. PostgreSQL là nguồn sự thật (sold, hold);
-- Valkey chỉ là cổng trừ suất nhanh khi đông người mua, mất dữ liệu thì khởi tạo lại từ quota - sold - đang giữ.

CREATE TABLE flash_sale
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(150)             NOT NULL,
    starts_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    status     VARCHAR(16)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64)              NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64)              NOT NULL,
    version    BIGINT                   NOT NULL DEFAULT 0,
    CHECK (ends_at > starts_at)
);
CREATE INDEX ix_flash_sale_window ON flash_sale (status, starts_at, ends_at);

CREATE TABLE flash_sale_item
(
    id                 UUID PRIMARY KEY,
    flash_sale_id      UUID                     NOT NULL REFERENCES flash_sale (id),
    sku_id             UUID                     NOT NULL REFERENCES product_variant (id),
    sale_price         NUMERIC(19, 2)           NOT NULL CHECK (sale_price >= 0),
    quota              INT                      NOT NULL CHECK (quota > 0),
    sold               INT                      NOT NULL DEFAULT 0 CHECK (sold >= 0),
    per_customer_limit INT                      NOT NULL DEFAULT 0 CHECK (per_customer_limit >= 0),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         VARCHAR(64)              NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by         VARCHAR(64)              NOT NULL,
    version            BIGINT                   NOT NULL DEFAULT 0,
    UNIQUE (flash_sale_id, sku_id)
);
CREATE INDEX ix_flash_sale_item_sku ON flash_sale_item (sku_id);

-- Suất đang giữ theo đơn: trả lại idempotent khi đơn hủy/hết hạn, chuyển COMMITTED khi đơn xác nhận
CREATE TABLE flash_sale_hold
(
    id                 UUID PRIMARY KEY,
    order_id           UUID                     NOT NULL,
    flash_sale_item_id UUID                     NOT NULL REFERENCES flash_sale_item (id),
    customer_id        VARCHAR(64),
    quantity           INT                      NOT NULL CHECK (quantity > 0),
    status             VARCHAR(16)              NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         VARCHAR(64)              NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by         VARCHAR(64)              NOT NULL,
    version            BIGINT                   NOT NULL DEFAULT 0,
    UNIQUE (order_id, flash_sale_item_id)
);
CREATE INDEX ix_flash_sale_hold_item_status ON flash_sale_hold (flash_sale_item_id, status);
