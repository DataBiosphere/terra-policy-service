# Terra Policy Service

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=terra-policy-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=terra-policy-service)

## Local Development

## Prod Deployment
* [Swagger UI](https://tps.dsde-prod.broadinstitute.org/)

### Setup
Run `./gradlew generateSwaggerCode` to generate the Swagger code for models and the Swagger UI.
Run `docker compose up` to start the postgres db locally.
Run `./gradlew test` to run the unit tests.
Run `./gradlew bootRun` to run the Policy Service locally (Swagger UI at localhost:8080).
Run `./gradlew service:dependencies --write-locks ` to rewrite the gradle dependency lock for the spring boot service.

## Run Pact Tests
To run the Pact tests, run the following:

```shell
export PACT_BROKER_URL="pact-broker.dsp-eng-tools.broadinstitute.org"
export PACT_PROVIDER_COMMIT="$(git rev-parse HEAD)"
export PACT_PROVIDER_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
export PACT_BROKER_USERNAME="$(vault read -field=basic_auth_read_only_username secret/dsp/pact-broker/users/read-only)"
export PACT_BROKER_PASSWORD="$(vault read -field=basic_auth_read_only_password secret/dsp/pact-broker/users/read-only)"

./gradlew verifyPacts
```
