-- Mỗi service một database và user riêng; user không có quyền vào database của service khác.
-- Mật khẩu chỉ dùng cho dev local.

CREATE USER customer WITH PASSWORD 'customer';
CREATE DATABASE customer_db OWNER customer;

CREATE USER catalog WITH PASSWORD 'catalog';
CREATE DATABASE catalog_db OWNER catalog;

CREATE USER orders WITH PASSWORD 'orders';
CREATE DATABASE order_db OWNER orders;

CREATE USER notification WITH PASSWORD 'notification';
CREATE DATABASE notification_db OWNER notification;

CREATE USER keycloak WITH PASSWORD 'keycloak';
CREATE DATABASE keycloak_db OWNER keycloak;

REVOKE CONNECT ON DATABASE customer_db, catalog_db, order_db, notification_db, keycloak_db FROM PUBLIC;
GRANT CONNECT ON DATABASE customer_db TO customer;
GRANT CONNECT ON DATABASE catalog_db TO catalog;
GRANT CONNECT ON DATABASE order_db TO orders;
GRANT CONNECT ON DATABASE notification_db TO notification;
GRANT CONNECT ON DATABASE keycloak_db TO keycloak;
