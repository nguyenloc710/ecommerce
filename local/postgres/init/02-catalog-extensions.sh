#!/bin/sh
# Extension cho tìm kiếm tiếng Việt không dấu ở catalog-service (cần quyền superuser để tạo).
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname catalog_db <<'SQL'
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
SQL
