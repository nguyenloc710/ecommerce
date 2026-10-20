-- customer-service: hồ sơ khách (id = sub Keycloak), sổ địa chỉ, wishlist, đơn vị hành chính, nhật ký thao tác admin.

CREATE TABLE customer_profile
(
    id         UUID PRIMARY KEY, -- = sub của Keycloak, không sinh ở ứng dụng
    email      VARCHAR(255)             NOT NULL,
    full_name  VARCHAR(150),
    phone      VARCHAR(20),
    birthday   DATE,
    gender     VARCHAR(10),
    status     VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64)              NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64)              NOT NULL,
    version    BIGINT                   NOT NULL DEFAULT 0
);
CREATE INDEX ix_customer_profile_email ON customer_profile (lower(email));
CREATE INDEX ix_customer_profile_phone ON customer_profile (phone);

-- Đơn vị hành chính 2 cấp (dữ liệu nạp bằng R__admin_divisions.sql)
CREATE TABLE admin_division
(
    code          VARCHAR(10) PRIMARY KEY,
    parent_code   VARCHAR(10) REFERENCES admin_division (code),
    level         SMALLINT     NOT NULL, -- 1 = tỉnh/thành, 2 = phường/xã
    name          VARCHAR(120) NOT NULL,
    division_type VARCHAR(40)  NOT NULL,
    codename      VARCHAR(120) NOT NULL
);
CREATE INDEX ix_admin_division_parent ON admin_division (parent_code, name);

CREATE TABLE address
(
    id             UUID PRIMARY KEY,
    customer_id    UUID                     NOT NULL REFERENCES customer_profile (id),
    recipient_name VARCHAR(150)             NOT NULL,
    phone          VARCHAR(20)              NOT NULL,
    province_code  VARCHAR(10)              NOT NULL REFERENCES admin_division (code),
    province_name  VARCHAR(120)             NOT NULL, -- snapshot để đơn hàng/hiển thị không phụ thuộc đổi tên
    ward_code      VARCHAR(10)              NOT NULL REFERENCES admin_division (code),
    ward_name      VARCHAR(120)             NOT NULL,
    line1          VARCHAR(255)             NOT NULL,
    note           VARCHAR(255),
    is_default     BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by     VARCHAR(64)              NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by     VARCHAR(64)              NOT NULL,
    deleted_at     TIMESTAMP WITH TIME ZONE,
    deleted_by     VARCHAR(64),
    version        BIGINT                   NOT NULL DEFAULT 0
);
CREATE INDEX ix_address_customer ON address (customer_id) WHERE deleted_at IS NULL;
-- Mỗi khách tối đa một địa chỉ mặc định còn hiệu lực
CREATE UNIQUE INDEX ux_address_default ON address (customer_id) WHERE is_default AND deleted_at IS NULL;

CREATE TABLE wishlist_item
(
    id           UUID PRIMARY KEY,
    customer_id  UUID                     NOT NULL REFERENCES customer_profile (id),
    sku_id       UUID                     NOT NULL,
    product_slug VARCHAR(160)             NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by   VARCHAR(64)              NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by   VARCHAR(64)              NOT NULL,
    version      BIGINT                   NOT NULL DEFAULT 0,
    UNIQUE (customer_id, sku_id)
);

-- Thao tác của nhân viên lên khách/nhân viên khác (khóa tài khoản, tạo nhân viên...), giữ 2 năm
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
