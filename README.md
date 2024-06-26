# Terra Policy Service

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=terra-policy-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=terra-policy-service)

## Local Development

## Prod Deployment
* [Swagger UI](https://tps.dsde-prod.broadinstitute.org/)

### Setup
Run `./gradlew generateSwaggerCode` to generate the Swagger code for models and the Swagger UI.
Run `docker compose up -d` to start the postgres db locally in the background.
Run `./gradlew test` to run the unit tests.
Run `./gradlew bootRun` to run the Policy Service locally (Swagger UI at localhost:8080).
Run `./gradlew service:dependencies --write-locks ` to rewrite the gradle dependency lock for the spring boot service.
Run `docker compose down` to stop the postgres db.
