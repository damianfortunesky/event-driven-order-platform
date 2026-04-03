#!/usr/bin/env bash
set -euo pipefail

required=(
  README.md
  .editorconfig
  .gitignore
  .env.example
  infra/docker-compose/docker-compose.local.yml
  deploy/k8s/base/kustomization.yaml
  contracts/events/topic-catalog.md
)

for path in "${required[@]}"; do
  [[ -e "$path" ]] || { echo "missing: $path"; exit 1; }
  echo "ok: $path"
done
