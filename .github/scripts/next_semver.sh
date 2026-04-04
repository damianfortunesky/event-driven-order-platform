#!/usr/bin/env bash
set -euo pipefail

prefix="$1" # e.g. order-service or contracts
latest_tag=$(git tag --list "${prefix}/v*" --sort=-v:refname | head -n1 || true)

if [[ -z "${latest_tag}" ]]; then
  echo "1.0.0"
  exit 0
fi

version="${latest_tag#${prefix}/v}"
IFS='.' read -r major minor patch <<< "${version}"
major=${major:-1}
minor=${minor:-0}
patch=${patch:-0}
patch=$((patch + 1))
echo "${major}.${minor}.${patch}"
