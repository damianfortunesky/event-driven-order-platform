CREATE TABLE IF NOT EXISTS inventory_stock (
    product_id UUID PRIMARY KEY,
    available_quantity BIGINT NOT NULL CHECK (available_quantity >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS inventory_reservation (
    id UUID PRIMARY KEY,
    payment_event_id UUID NOT NULL UNIQUE,
    order_id UUID NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(255),
    correlation_id VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS inventory_reservation_item (
    reservation_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (reservation_id, product_id),
    CONSTRAINT fk_reservation_item_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES inventory_reservation(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_reservation_item_stock
        FOREIGN KEY (product_id)
        REFERENCES inventory_stock(product_id)
);

CREATE TABLE IF NOT EXISTS inventory_inbox_event (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(120) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_order_id ON inventory_reservation(order_id);
