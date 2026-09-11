SHELL := /bin/bash

.PHONY: run test test-unit test-integration build mysql rabbitmq-status rabbitmq-purge analytics kill-8080 check-env

REQUIRED_VARS = DB_PASSWORD GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET MAIL_USERNAME MAIL_PASSWORD

check-env:
	@for v in $(REQUIRED_VARS); do \
	   if [ -z "$${!v}" ]; then \
	      echo "Missing required env var: $$v (see .env.example, then: source .env)"; \
	      exit 1; \
	   fi; \
	done

run: check-env
	./mvnw spring-boot:run

test: check-env
	./mvnw test

test-unit: check-env

test-integration: check-env
	./mvnw test -Dtest='*IntegrationTest'

build:
	./mvnw clean compile

mysql:
	mysql -u hr_app -p hr_system

rabbitmq-status:
	sudo rabbitmqctl list_queues name messages

rabbitmq-purge:
	sudo rabbitmqctl purge_queue notification.queue

analytics: check-env
	cd ../analytics && \
	source venv/bin/activate && \
	python3 compute_metrics.py

kill-8080:
	sudo lsof -i :8080
	sudo fuser -k 8080/tcp
