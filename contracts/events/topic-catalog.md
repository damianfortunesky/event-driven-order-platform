# Topic Catalog v1

| Topic | Key | Producer | Consumers |
|---|---|---|---|
| `orders.order-created.v1` | `orderId` | order-service | payment-service |
| `payments.payment-approved.v1` | `orderId` | payment-service | order-service, inventory-service |
| `payments.payment-rejected.v1` | `orderId` | payment-service | order-service |
| `inventory.inventory-reserved.v1` | `orderId` | inventory-service | order-service, shipping-service |
| `inventory.inventory-failed.v1` | `orderId` | inventory-service | order-service |
| `orders.order-completed.v1` | `orderId` | order-service | notification-service |
| `orders.order-failed.v1` | `orderId` | order-service | notification-service |
