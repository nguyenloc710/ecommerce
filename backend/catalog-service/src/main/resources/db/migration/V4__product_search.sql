-- Tìm kiếm tiếng Việt không dấu bằng PostgreSQL full-text + trigram (thay OpenSearch ở MVP).
-- unaccent và pg_trgm là trusted extension (PG13+) nên owner database tạo được, không cần superuser.
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- unaccent() là STABLE nên không dùng được trong cột generated/index; bọc lại thành IMMUTABLE với dictionary cố định.
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
    RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT public.unaccent('public.unaccent', $1) $$;

ALTER TABLE product
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', immutable_unaccent(coalesce(name, ''))), 'A') ||
        setweight(to_tsvector('simple', immutable_unaccent(coalesce(description, ''))), 'C')
        ) STORED;
CREATE INDEX ix_product_search_vector ON product USING gin (search_vector);
-- Gõ dở / sai chính tả nhẹ: so khớp trigram trên tên đã bỏ dấu
CREATE INDEX ix_product_name_trgm ON product USING gin (immutable_unaccent(lower(name)) gin_trgm_ops);
