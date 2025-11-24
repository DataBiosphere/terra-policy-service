FROM postgres:17.7-alpine
COPY ../scripts/postgres-init.sql /docker-entrypoint-initdb.d/
