-- Chạy một lần khi tạo volume Postgres lần đầu.
-- Bảng do Flyway trong backend tạo (backend/src/main/resources/db/migration).
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
