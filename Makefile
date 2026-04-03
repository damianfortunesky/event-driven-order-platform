SHELL := /bin/bash

.PHONY: help bootstrap up down restart logs topics lint tree ps

help:
	@echo "Targets: bootstrap up down restart logs topics ps lint tree"

bootstrap:
	./scripts/bootstrap.sh

up:
	docker compose --env-file .env up -d

down:
	docker compose --env-file .env down -v

restart:
	docker compose --env-file .env down && docker compose --env-file .env up -d

logs:
	docker compose --env-file .env logs -f

ps:
	docker compose --env-file .env ps

topics:
	./scripts/create-topics.sh

lint:
	./scripts/verify-structure.sh

tree:
	find . -maxdepth 4 -type d | sort
