#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-kafka:9092}"
PARTITIONS="${KAFKA_DEFAULT_PARTITIONS:-3}"
REPLICATION_FACTOR="${KAFKA_REPLICATION_FACTOR:-1}"

TOPICS=(
  orders.order-created.v1
  payments.payment-approved.v1
  payments.payment-rejected.v1
  inventory.inventory-reserved.v1
  inventory.inventory-failed.v1
  orders.order-completed.v1
  orders.order-failed.v1
)

echo "Waiting for Kafka at ${BOOTSTRAP_SERVER}..."
until /opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --list >/dev/null 2>&1; do
  sleep 2
done

echo "Kafka is up. Ensuring topics exist..."
for topic in "${TOPICS[@]}"; do
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions "${PARTITIONS}" \
    --replication-factor "${REPLICATION_FACTOR}"
  echo "topic ready: ${topic}"
done

echo "Done."
