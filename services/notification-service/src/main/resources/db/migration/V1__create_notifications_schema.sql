CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    source_event_id UUID NOT NULL,
    source_event_type VARCHAR(120) NOT NULL,
    order_id VARCHAR(120) NOT NULL,
    customer_id VARCHAR(120),
    recipient_email VARCHAR(255) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    correlation_id VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_notifications_source_event UNIQUE (source_event_id, source_event_type)
);

CREATE INDEX IF NOT EXISTS idx_notifications_order_id ON notifications(order_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
