-- Bảng outbox của Spring Modulith (Event Publication Registry, schema v2 cho PostgreSQL).
-- Event được ghi vào đây trong cùng transaction với dữ liệu nghiệp vụ, sau đó mới gửi sang RabbitMQ.
CREATE TABLE IF NOT EXISTS event_publication
(
    id                     UUID                     NOT NULL,
    listener_id            TEXT                     NOT NULL,
    event_type             TEXT                     NOT NULL,
    serialized_event       TEXT                     NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 TEXT,
    completion_attempts    INT,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);

-- Event đã xử lý, để consumer idempotent khi RabbitMQ gửi lại cùng một event.
CREATE TABLE processed_event
(
    event_id     UUID                     NOT NULL,
    consumer     VARCHAR(100)             NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer)
);
