CREATE TABLE IF NOT EXISTS payments (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL UNIQUE,
  order_id UUID NOT NULL,
  total_amount NUMERIC(19,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  status VARCHAR(16) NOT NULL,
  failure_reason VARCHAR(255),
  correlation_id VARCHAR(128) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);
