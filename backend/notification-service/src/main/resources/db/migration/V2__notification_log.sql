-- Nhật ký thông báo đã gửi; idempotent theo (event_id, channel): một event chỉ gửi một lần trên mỗi kênh.
CREATE TABLE notification_log
(
    id         UUID PRIMARY KEY,
    event_id   UUID                     NOT NULL,
    kind       VARCHAR(40)              NOT NULL, -- ORDER_CONFIRMED, ORDER_SHIPPED, CUSTOMER_WELCOME...
    channel    VARCHAR(20)              NOT NULL, -- EMAIL (ZALO, SMS ở giai đoạn 3)
    recipient  VARCHAR(255)             NOT NULL,
    subject    VARCHAR(255),
    status     VARCHAR(20)              NOT NULL, -- SENT | SKIPPED
    detail     VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX ux_notification_log_event_channel ON notification_log (event_id, channel);
CREATE INDEX ix_notification_log_recipient ON notification_log (recipient, created_at DESC);
