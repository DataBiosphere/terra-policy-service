FROM postgres:13.1-alpine
COPY ../scripts/postgres-init.sql /docker-entrypoint-initdb.d/
