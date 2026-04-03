SHELL := /bin/bash

.PHONY: help bootstrap up down logs topics lint tree

help:
	@echo "Targets: bootstrap up down logs topics lint tree"

bootstrap:
	./scripts/bootstrap.sh

up:
	docker compose --env-file .env -f infra/docker-compose/docker-compose.local.yml up -d

down:
	docker compose --env-file .env -f infra/docker-compose/docker-compose.local.yml down -v

logs:
	docker compose --env-file .env -f infra/docker-compose/docker-compose.local.yml logs -f

topics:
	./scripts/create-topics.sh

lint:
	./scripts/verify-structure.sh

tree:
	find . -maxdepth 4 -type d | sort
