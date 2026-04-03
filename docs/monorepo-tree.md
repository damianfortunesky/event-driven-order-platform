# Árbol del monorepo

```text
event-driven-order-platform/
├── .editorconfig
├── .env.example
├── .gitignore
├── Makefile
├── README.md
├── contracts/
│   ├── README.md
│   ├── envelopes/
│   │   └── event-envelope-v1.json
│   ├── events/
│   │   └── topic-catalog.md
│   └── schemas/
│       └── order-created.v1.json
├── deploy/
│   ├── README.md
│   └── k8s/
│       ├── base/
│       │   ├── inventory-service.yaml
│       │   ├── kustomization.yaml
│       │   ├── namespace.yaml
│       │   ├── notification-service.yaml
│       │   ├── order-service.yaml
│       │   ├── payment-service.yaml
│       │   └── shipping-service.yaml
│       └── overlays/
│           ├── dev/
│           │   └── kustomization.yaml
│           └── local/
│               └── kustomization.yaml
├── docs/
│   ├── README.md
│   ├── adr/
│   │   ├── 0001-topic-naming.md
│   │   ├── 0002-outbox-pattern.md
│   │   └── 0003-retry-dlq-strategy.md
│   ├── blueprint-eda-kafka.md
│   ├── diagrams/
│   │   ├── context-map.mmd
│   │   ├── deployment-k8s.mmd
│   │   └── event-flow-sequence.mmd
│   ├── monorepo-tree.md
│   └── runbooks/
│       ├── incident-dlq-reprocessing.md
│       ├── kafka-troubleshooting.md
│       └── local-dev.md
├── infra/
│   ├── README.md
│   ├── docker-compose/
│   │   └── docker-compose.local.yml
│   ├── kafka/
│   │   └── create-topics.sh
│   └── postgres/
│       └── README.md
├── observability/
│   ├── README.md
│   ├── dashboards/
│   │   └── README.md
│   ├── grafana/
│   │   ├── dashboards/
│   │   └── provisioning/
│   │       └── datasources.yml
│   └── prometheus/
│       └── prometheus.yml
├── scripts/
│   ├── README.md
│   ├── bootstrap.sh
│   ├── create-topics.sh
│   └── verify-structure.sh
└── services/
    ├── inventory-service/
    │   ├── README.md
    │   ├── pom.xml
    │   └── src/
    │       ├── main/
    │       │   ├── java/com/eventdriven/inventory/InventoryServiceApplication.java
    │       │   └── resources/application.yml
    │       └── test/java/com/eventdriven/inventory/
    ├── notification-service/
    │   ├── README.md
    │   ├── pom.xml
    │   └── src/
    │       ├── main/
    │       │   ├── java/com/eventdriven/notification/NotificationServiceApplication.java
    │       │   └── resources/application.yml
    │       └── test/java/com/eventdriven/notification/
    ├── order-service/
    │   ├── README.md
    │   ├── pom.xml
    │   └── src/
    │       ├── main/
    │       │   ├── java/com/eventdriven/order/OrderServiceApplication.java
    │       │   └── resources/application.yml
    │       └── test/java/com/eventdriven/order/
    ├── payment-service/
    │   ├── README.md
    │   ├── pom.xml
    │   └── src/
    │       ├── main/
    │       │   ├── java/com/eventdriven/payment/PaymentServiceApplication.java
    │       │   └── resources/application.yml
    │       └── test/java/com/eventdriven/payment/
    └── shipping-service/
        ├── README.md
        ├── pom.xml
        └── src/
            ├── main/
            │   ├── java/com/eventdriven/shipping/ShippingServiceApplication.java
            │   └── resources/application.yml
            └── test/java/com/eventdriven/shipping/
```
