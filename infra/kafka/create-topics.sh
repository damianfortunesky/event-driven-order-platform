#!/usr/bin/env bash
set -euo pipefail

TOPICS=(
  orders.order-created.v1
  payments.payment-approved.v1
  payments.payment-rejected.v1
  inventory.inventory-reserved.v1
  inventory.inventory-failed.v1
  orders.order-completed.v1
  orders.order-failed.v1
)

for topic in "${TOPICS[@]}"; do
  docker exec -i $(docker ps --filter name=kafka --format '{{.ID}}' | head -n1) \
    kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic "$topic" --partitions 3 --replication-factor 1 || true
  echo "topic ready: $topic"
done
