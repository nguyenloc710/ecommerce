-- Bảng audit thao tác nhân viên dùng chung (entity AdminAuditLog trong common-jpa; ddl-auto=validate cần bảng này ở mọi service)
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
