CREATE TABLE payments (
    id               VARCHAR(36)    PRIMARY KEY,
    amount           NUMERIC(19, 2) NOT NULL,
    currency         VARCHAR(8)     NOT NULL,
    payment_method   VARCHAR(32)    NOT NULL,
    idempotency_key  VARCHAR(128),
    status           VARCHAR(16)    NOT NULL,
    created_at       TIMESTAMP      NOT NULL,
    updated_at       TIMESTAMP      NOT NULL,
    CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_payments_status      ON payments (status);
CREATE INDEX idx_payments_created_at  ON payments (created_at);

CREATE TABLE refunds (
    id          VARCHAR(36)    PRIMARY KEY,
    payment_id  VARCHAR(36)    NOT NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    reason      VARCHAR(255),
    status      VARCHAR(16)    NOT NULL,
    created_at  TIMESTAMP      NOT NULL,
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);
