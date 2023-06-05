FROM postgres:13.1-alpine
COPY postgres-init.sql /docker-entrypoint-initdb.d/
