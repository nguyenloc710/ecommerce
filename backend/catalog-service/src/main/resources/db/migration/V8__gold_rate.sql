-- Giá vàng theo ngày (E7.3, B-09): sản phẩm GOLD_RATE có giá variant = trọng lượng vàng × giá vàng hiện hành của loại
-- vàng + phí gia công, làm tròn. Nhập giá vàng mới thì tính lại và ghi vào product_variant.price (Envers giữ lịch sử),
-- nên danh sách, lọc theo giá, checkout đều đọc cùng một giá.

-- Chỉ thêm, không sửa: bản ghi mới nhất theo effective_at của mỗi loại vàng là giá hiện hành
CREATE TABLE gold_rate
(
    id             UUID PRIMARY KEY,
    karat          VARCHAR(8)               NOT NULL,
    price_per_gram NUMERIC(19, 2)           NOT NULL CHECK (price_per_gram > 0),
    effective_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    note           VARCHAR(255),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by     VARCHAR(64)              NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by     VARCHAR(64)              NOT NULL,
    version        BIGINT                   NOT NULL DEFAULT 0
);
CREATE INDEX ix_gold_rate_karat_effective ON gold_rate (karat, effective_at DESC);

ALTER TABLE product_variant
    ADD COLUMN gold_karat  VARCHAR(8),
    ADD COLUMN gold_weight NUMERIC(10, 3) CHECK (gold_weight > 0),
    ADD COLUMN making_fee  NUMERIC(19, 2) CHECK (making_fee >= 0);
CREATE INDEX ix_product_variant_gold_karat ON product_variant (gold_karat) WHERE gold_karat IS NOT NULL;

ALTER TABLE product_variant_aud
    ADD COLUMN gold_karat  VARCHAR(8),
    ADD COLUMN gold_weight NUMERIC(10, 3),
    ADD COLUMN making_fee  NUMERIC(19, 2);
