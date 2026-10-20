-- Module product: danh mục, thương hiệu, sản phẩm, biến thể (SKU), ảnh.
-- Quy ước: audit cột + version ở mọi bảng; xóa mềm bằng deleted_at (unique là partial index).

CREATE TABLE category
(
    id                 UUID PRIMARY KEY,
    parent_id          UUID REFERENCES category (id),
    name               VARCHAR(120)             NOT NULL,
    slug               VARCHAR(140)             NOT NULL,
    description        TEXT,
    attribute_template JSONB                    NOT NULL DEFAULT '{}'::jsonb,
    sort_order         INT                      NOT NULL DEFAULT 0,
    status             VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         VARCHAR(64)              NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by         VARCHAR(64)              NOT NULL,
    deleted_at         TIMESTAMP WITH TIME ZONE,
    deleted_by         VARCHAR(64),
    version            BIGINT                   NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_category_slug_live ON category (slug) WHERE deleted_at IS NULL;
CREATE INDEX ix_category_parent ON category (parent_id);

CREATE TABLE brand
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(120)             NOT NULL,
    slug        VARCHAR(140)             NOT NULL,
    logo_url    VARCHAR(500),
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(64)              NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by  VARCHAR(64)              NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    deleted_by  VARCHAR(64),
    version     BIGINT                   NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_brand_slug_live ON brand (slug) WHERE deleted_at IS NULL;

CREATE TABLE product
(
    id              UUID PRIMARY KEY,
    category_id     UUID                     NOT NULL REFERENCES category (id),
    brand_id        UUID REFERENCES brand (id),
    slug            VARCHAR(160)             NOT NULL,
    name            VARCHAR(200)             NOT NULL,
    description     TEXT,
    attributes      JSONB                    NOT NULL DEFAULT '{}'::jsonb,
    pricing_type    VARCHAR(20)              NOT NULL DEFAULT 'FIXED',
    status          VARCHAR(20)              NOT NULL DEFAULT 'DRAFT',
    seo_title       VARCHAR(200),
    seo_description VARCHAR(300),
    published_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(64)              NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by      VARCHAR(64)              NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    deleted_by      VARCHAR(64),
    version         BIGINT                   NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_product_slug_live ON product (slug) WHERE deleted_at IS NULL;
CREATE INDEX ix_product_category_status ON product (category_id, status) WHERE deleted_at IS NULL;
CREATE INDEX ix_product_brand ON product (brand_id);
CREATE INDEX ix_product_published ON product (published_at DESC);
CREATE INDEX ix_product_attributes ON product USING gin (attributes);

CREATE TABLE product_variant
(
    id               UUID PRIMARY KEY,
    product_id       UUID                     NOT NULL REFERENCES product (id),
    sku_code         VARCHAR(64)              NOT NULL,
    option_values    JSONB                    NOT NULL DEFAULT '{}'::jsonb,
    price            NUMERIC(19, 2)           NOT NULL,
    compare_at_price NUMERIC(19, 2),
    weight_gram      INT,
    barcode          VARCHAR(64),
    status           VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    sort_order       INT                      NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by       VARCHAR(64)              NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by       VARCHAR(64)              NOT NULL,
    deleted_at       TIMESTAMP WITH TIME ZONE,
    deleted_by       VARCHAR(64),
    version          BIGINT                   NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_variant_sku_live ON product_variant (sku_code) WHERE deleted_at IS NULL;
CREATE INDEX ix_variant_product ON product_variant (product_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_variant_option_values ON product_variant USING gin (option_values);

-- Ảnh không được tham chiếu từ nơi khác (đơn hàng snapshot URL) nên xóa cứng, chỉ giữ audit tạo/sửa.
CREATE TABLE product_image
(
    id         UUID PRIMARY KEY,
    product_id UUID                     NOT NULL REFERENCES product (id),
    variant_id UUID REFERENCES product_variant (id),
    url        VARCHAR(500)             NOT NULL,
    alt        VARCHAR(200),
    sort_order INT                      NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64)              NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64)              NOT NULL,
    version    BIGINT                   NOT NULL DEFAULT 0
);
CREATE INDEX ix_image_product ON product_image (product_id, sort_order);

-- Hibernate Envers: lịch sử thay đổi product và product_variant (giá, tên, trạng thái).
CREATE TABLE revinfo
(
    id        BIGSERIAL PRIMARY KEY,
    timestamp BIGINT      NOT NULL,
    username  VARCHAR(64) NOT NULL
);

CREATE TABLE product_aud
(
    id              UUID     NOT NULL,
    rev             BIGINT   NOT NULL REFERENCES revinfo (id),
    revtype         SMALLINT,
    category_id     UUID,
    brand_id        UUID,
    slug            VARCHAR(160),
    name            VARCHAR(200),
    description     TEXT,
    attributes      JSONB,
    pricing_type    VARCHAR(20),
    status          VARCHAR(20),
    seo_title       VARCHAR(200),
    seo_description VARCHAR(300),
    published_at    TIMESTAMP WITH TIME ZONE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    deleted_by      VARCHAR(64),
    PRIMARY KEY (id, rev)
);

CREATE TABLE product_variant_aud
(
    id               UUID     NOT NULL,
    rev              BIGINT   NOT NULL REFERENCES revinfo (id),
    revtype          SMALLINT,
    product_id       UUID,
    sku_code         VARCHAR(64),
    option_values    JSONB,
    price            NUMERIC(19, 2),
    compare_at_price NUMERIC(19, 2),
    weight_gram      INT,
    barcode          VARCHAR(64),
    status           VARCHAR(20),
    sort_order       INT,
    deleted_at       TIMESTAMP WITH TIME ZONE,
    deleted_by       VARCHAR(64),
    PRIMARY KEY (id, rev)
);
