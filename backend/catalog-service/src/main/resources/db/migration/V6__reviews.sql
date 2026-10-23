-- Module review (E7.1, B-10): khách chỉ đánh giá sản phẩm đã nhận hàng; đánh giá chờ duyệt tay trước khi hiện.

-- Quyền đánh giá mở khi nhận event order.delivered (mỗi SKU trong đơn một dòng, idempotent theo (order_id, sku_id))
CREATE TABLE review_eligibility
(
    id           UUID PRIMARY KEY,
    customer_id  VARCHAR(64)              NOT NULL,
    product_id   UUID                     NOT NULL REFERENCES product (id),
    sku_id       UUID                     NOT NULL REFERENCES product_variant (id),
    order_id     UUID                     NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by   VARCHAR(64)              NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by   VARCHAR(64)              NOT NULL,
    version      BIGINT                   NOT NULL DEFAULT 0,
    UNIQUE (order_id, sku_id)
);
CREATE INDEX ix_review_eligibility_customer_product ON review_eligibility (customer_id, product_id);

-- Mỗi khách một đánh giá cho mỗi sản phẩm. Ẩn đánh giá = chuyển REJECTED (không xóa).
-- image_keys: object key trên kho ảnh (R2/S3), URL công khai ghép lúc đọc
CREATE TABLE review
(
    id             UUID PRIMARY KEY,
    product_id     UUID                     NOT NULL REFERENCES product (id),
    sku_id         UUID                     NOT NULL REFERENCES product_variant (id),
    customer_id    VARCHAR(64)              NOT NULL,
    order_id       UUID                     NOT NULL,
    author_name    VARCHAR(100)             NOT NULL,
    rating         SMALLINT                 NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content        VARCHAR(2000),
    image_keys     JSONB                    NOT NULL DEFAULT '[]',
    status         VARCHAR(16)              NOT NULL,
    reject_reason  VARCHAR(255),
    moderated_by   VARCHAR(64),
    moderated_at   TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by     VARCHAR(64)              NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by     VARCHAR(64)              NOT NULL,
    version        BIGINT                   NOT NULL DEFAULT 0,
    UNIQUE (customer_id, product_id)
);
-- Trang sản phẩm đọc đánh giá đã duyệt mới nhất; admin lọc theo trạng thái
CREATE INDEX ix_review_product_status ON review (product_id, status, created_at DESC);
CREATE INDEX ix_review_status_created ON review (status, created_at);
