-- V3: Stripe integration schema changes

-- Add Stripe fields to payments
ALTER TABLE payments ADD COLUMN stripe_payment_intent_id VARCHAR(128);
ALTER TABLE payments ADD COLUMN stripe_client_secret VARCHAR(255);

-- Index for searching payments by Stripe PaymentIntent ID during webhook callbacks
CREATE INDEX idx_payments_stripe_pi_id ON payments (stripe_payment_intent_id);

-- Create table to track processed Stripe Event IDs to prevent duplicate webhook handling
CREATE TABLE processed_stripe_events (
    event_id     VARCHAR(64) PRIMARY KEY,
    processed_at TIMESTAMP   NOT NULL
);
