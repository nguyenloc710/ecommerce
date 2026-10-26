-- Hoàn tiền tay (B-33): đơn đã hủy nhưng đã nhận tiền online. Một payment có tối đa một yêu cầu hoàn.
CREATE TABLE payment_refund
(
    id              UUID PRIMARY KEY,
    payment_id      UUID                     NOT NULL UNIQUE REFERENCES payment (id),
    order_id        UUID                     NOT NULL REFERENCES orders (id),
    order_code      VARCHAR(30)              NOT NULL,
    provider        VARCHAR(20)              NOT NULL,
    provider_txn_id VARCHAR(100),
    amount          NUMERIC(19, 2)           NOT NULL,
    cause           VARCHAR(30)              NOT NULL, -- PAID_AFTER_CANCEL | CANCELLED_AFTER_PAID
    cancel_reason   VARCHAR(255),
    status          VARCHAR(20)              NOT NULL, -- PENDING | DONE
    reference       VARCHAR(100),                      -- mã giao dịch hoàn bên cổng/ngân hàng
    note            VARCHAR(255),
    completed_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(64)              NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by      VARCHAR(64)              NOT NULL,
    version         BIGINT                   NOT NULL DEFAULT 0
);
CREATE INDEX ix_payment_refund_status ON payment_refund (status, created_at);
