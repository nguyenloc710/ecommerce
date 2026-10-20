-- order-service: giỏ hàng, voucher, đơn hàng (snapshot), lịch sử trạng thái, thanh toán, vận đơn, audit admin.

-- Giỏ của khách đã đăng nhập (khách vãng lai giữ ở trình duyệt, gộp khi đăng nhập)
CREATE TABLE cart
(
    id          UUID PRIMARY KEY,
    customer_id UUID                     NOT NULL UNIQUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(64)              NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by  VARCHAR(64)              NOT NULL,
    version     BIGINT                   NOT NULL DEFAULT 0
);

CREATE TABLE cart_item
(
    id           UUID PRIMARY KEY,
    cart_id      UUID                     NOT NULL REFERENCES cart (id) ON DELETE CASCADE,
    sku_id       UUID                     NOT NULL,
    product_slug VARCHAR(160)             NOT NULL,
    engraving    VARCHAR(10),            -- B-08: khắc tên tối đa 10 ký tự, NULL = không khắc
    quantity     INT                      NOT NULL CHECK (quantity > 0),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by   VARCHAR(64)              NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by   VARCHAR(64)              NOT NULL,
    version      BIGINT                   NOT NULL DEFAULT 0
);
-- Cùng SKU nhưng khác nội dung khắc là hai dòng khác nhau
CREATE UNIQUE INDEX ux_cart_item_line ON cart_item (cart_id, sku_id, COALESCE(engraving, ''));

CREATE TABLE voucher
(
    code               VARCHAR(30)              NOT NULL,
    id                 UUID PRIMARY KEY,
    type               VARCHAR(10)              NOT NULL, -- PERCENT | FIXED
    value              NUMERIC(19, 2)           NOT NULL,
    max_discount       NUMERIC(19, 2),                    -- trần giảm cho PERCENT
    min_subtotal       NUMERIC(19, 2)           NOT NULL DEFAULT 0,
    starts_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    usage_limit        INT,                               -- NULL = không giới hạn
    used_count         INT                      NOT NULL DEFAULT 0,
    per_customer_limit INT                      NOT NULL DEFAULT 1,
    status             VARCHAR(10)              NOT NULL DEFAULT 'ACTIVE',
    description        VARCHAR(255),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         VARCHAR(64)              NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by         VARCHAR(64)              NOT NULL,
    deleted_at         TIMESTAMP WITH TIME ZONE,
    deleted_by         VARCHAR(64),
    version            BIGINT                   NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_voucher_code ON voucher (upper(code)) WHERE deleted_at IS NULL;

CREATE TABLE voucher_usage
(
    id          UUID PRIMARY KEY,
    voucher_id  UUID                     NOT NULL REFERENCES voucher (id),
    order_id    UUID                     NOT NULL UNIQUE,
    customer_id UUID                     NOT NULL,
    discount    NUMERIC(19, 2)           NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_voucher_usage_customer ON voucher_usage (voucher_id, customer_id);

-- B-02: mã đơn ORD-yymmdd-NNNN, NNNN tăng theo ngày
CREATE TABLE order_code_seq
(
    day     DATE PRIMARY KEY,
    last_no INT NOT NULL
);

CREATE TABLE orders
(
    id               UUID PRIMARY KEY,
    code             VARCHAR(20)              NOT NULL UNIQUE,
    customer_id      UUID                     NOT NULL,
    status           VARCHAR(20)              NOT NULL,
    subtotal         NUMERIC(19, 2)           NOT NULL,
    discount         NUMERIC(19, 2)           NOT NULL DEFAULT 0,
    shipping_fee     NUMERIC(19, 2)           NOT NULL DEFAULT 0,
    total            NUMERIC(19, 2)           NOT NULL,
    voucher_code     VARCHAR(30),
    recipient_name   VARCHAR(150)             NOT NULL,
    contact_phone    VARCHAR(20)              NOT NULL,
    contact_email    VARCHAR(255)             NOT NULL,
    shipping_address JSONB                    NOT NULL, -- snapshot: line1, wardName, provinceName, mã...
    note             VARCHAR(500),
    payment_method   VARCHAR(20)              NOT NULL, -- COD | MOCK_ONLINE | VNPAY | MOMO
    shipping_carrier VARCHAR(20)              NOT NULL,
    idempotency_key  VARCHAR(80)              NOT NULL,
    placed_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at          TIMESTAMP WITH TIME ZONE,
    cancelled_at     TIMESTAMP WITH TIME ZONE,
    cancel_reason    VARCHAR(255),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by       VARCHAR(64)              NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by       VARCHAR(64)              NOT NULL,
    version          BIGINT                   NOT NULL DEFAULT 0,
    UNIQUE (customer_id, idempotency_key)
);
CREATE INDEX ix_orders_customer ON orders (customer_id, placed_at DESC);
CREATE INDEX ix_orders_status ON orders (status, placed_at DESC);

-- Snapshot dòng hàng tại thời điểm đặt; không join sang catalog
CREATE TABLE order_item
(
    id            UUID PRIMARY KEY,
    order_id      UUID                     NOT NULL REFERENCES orders (id),
    sku_id        UUID                     NOT NULL,
    product_id    UUID                     NOT NULL,
    product_slug  VARCHAR(160)             NOT NULL,
    product_name  VARCHAR(200)             NOT NULL,
    variant_label VARCHAR(200),
    sku_code      VARCHAR(64),
    image_url     VARCHAR(500),
    unit_price    NUMERIC(19, 2)           NOT NULL,
    quantity      INT                      NOT NULL CHECK (quantity > 0),
    engraving     VARCHAR(10),
    line_total    NUMERIC(19, 2)           NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(64)              NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by    VARCHAR(64)              NOT NULL,
    version       BIGINT                   NOT NULL DEFAULT 0
);
CREATE INDEX ix_order_item_order ON order_item (order_id);

CREATE TABLE order_status_history
(
    id          UUID PRIMARY KEY,
    order_id    UUID                     NOT NULL REFERENCES orders (id),
    from_status VARCHAR(20),
    to_status   VARCHAR(20)              NOT NULL,
    reason      VARCHAR(255),
    actor_id    VARCHAR(64)              NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_order_status_history_order ON order_status_history (order_id, created_at);

CREATE TABLE payment
(
    id              UUID PRIMARY KEY,
    order_id        UUID                     NOT NULL REFERENCES orders (id),
    provider        VARCHAR(20)              NOT NULL, -- COD | MOCK_ONLINE | VNPAY | MOMO
    amount          NUMERIC(19, 2)           NOT NULL,
    status          VARCHAR(20)              NOT NULL, -- INIT | PENDING | SUCCEEDED | FAILED
    provider_txn_id VARCHAR(100),
    pay_url         VARCHAR(1000),
    raw_callback    JSONB,
    paid_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(64)              NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by      VARCHAR(64)              NOT NULL,
    version         BIGINT                   NOT NULL DEFAULT 0
);
CREATE INDEX ix_payment_order ON payment (order_id);
-- Webhook idempotent theo mã giao dịch của đối tác
CREATE UNIQUE INDEX ux_payment_provider_txn ON payment (provider, provider_txn_id) WHERE provider_txn_id IS NOT NULL;

CREATE TABLE shipment
(
    id            UUID PRIMARY KEY,
    order_id      UUID                     NOT NULL REFERENCES orders (id),
    carrier       VARCHAR(20)              NOT NULL,
    tracking_code VARCHAR(64)              NOT NULL,
    fee           NUMERIC(19, 2)           NOT NULL,
    status        VARCHAR(20)              NOT NULL, -- CREATED | IN_TRANSIT | DELIVERED | FAILED
    raw           JSONB,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(64)              NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by    VARCHAR(64)              NOT NULL,
    version       BIGINT                   NOT NULL DEFAULT 0
);
CREATE INDEX ix_shipment_order ON shipment (order_id);
CREATE UNIQUE INDEX ux_shipment_tracking ON shipment (carrier, tracking_code);

CREATE TABLE admin_audit_log
(
    id          UUID PRIMARY KEY,
    actor_id    VARCHAR(64)              NOT NULL,
    action      VARCHAR(60)              NOT NULL,
    target_type VARCHAR(40)              NOT NULL,
    target_id   VARCHAR(64)              NOT NULL,
    details     JSONB,
    trace_id    VARCHAR(64),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_admin_audit_target ON admin_audit_log (target_type, target_id, created_at DESC);

-- Bảng revision của Hibernate Envers (common-jpa khai báo AuditRevisionEntity nên ddl-auto=validate cần bảng này)
CREATE TABLE revinfo
(
    id        BIGSERIAL PRIMARY KEY,
    timestamp BIGINT      NOT NULL,
    username  VARCHAR(64) NOT NULL
);
