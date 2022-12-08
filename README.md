# Ory Hydra Reference Implementation - Java

This is an _unofficial_ reference implementation of the User Login and Consent flow of an 
[Ory Hydra](https://github.com/ory) OAuth 2.0 server written in Java with SpringBoot. This project demos some
key features/flows/integrations and aims to be a foundation for production implementations but it is not an exhaustive
implementation nor is guaranteed to be secure, bug free, fully tested, or production ready.

Similar reference implementations can be found on the [Getting Started](https://www.ory.sh/docs/getting-started/overview)
page of the official Ory website.

## Introduction

Ory Hydra is an open source project OpenID Certified OAuth 2.0 Server and OpenID Connect Provider. Ory Hydra is not an
identity provider (user sign up, user login, password reset flow), but connects to your existing identity provider
through a login and consent app. This project is an example of such a login and consent app. It aims to be a useful
reference for other Java implementations, providing insight into not only how to integrate with Ory Hydra but also how
to effectively test the entirety of the system using SpringBootTest, Test Containers, and Playwright.

The following features have been implemented:
- [Login](https://www.ory.sh/docs/hydra/guides/login)
- [Consent](https://www.ory.sh/docs/hydra/guides/consent)
- [OIDC](https://www.ory.sh/docs/hydra/concepts/openid-connect-oidc) (with custom claims)

Similar products include but are not limited to:
- [Spring Authorization Server](https://spring.io/projects/spring-authorization-server)
- [Auth0](https://auth0.com/docs/authenticate/protocols/oauth)
- [Keycloak](https://www.keycloak.org/)
- [Amazon Cognito](https://docs.aws.amazon.com/cognito/index.html)
- [Dex](https://dexidp.io/)

## Prerequisites: What do you need to get started?

- Java 17+
- Docker (only required for running tests)

## Technologies Used

- Java 17+
- SpringBoot
- Gradle
- Test Containers
- Ory Hydra
- Docker
- Freemarker
- Lombok
- GitHub Actions

## Running

### Running Functional Tests

### Running With Local Ory Hydra

## Example Flows

### Full OAuth Flow With OIDC

### Use 'Remember Me' To Skip Consent Screen

## GitHub Actions

## Playwright

## Test Containers

## Task List

- [ ] Add documentation explain application with screenshots
- [ ] Add a fancier UI
- [ ] Clean up integration tests
- [ ] Allow rejecting on consent screen
- [ ] Add more unit tests
- [ ] Document GitHub actions
- [ ] Document OIDC usage
- [ ] Document playwright usage
- [ ] Show login errors on login screen
- [ ] Add playwright traces https://playwright.dev/java/docs/trace-viewer-intro
- [ ] Document how to run locally with a local docker hydra instance
- [ ] Log out
- [ ] Add example with Ory Cloud

## Notes

docker run -d \
--name ory-hydra-example-sqlite \
--volume source=hydra-sqlite,target=/var/lib/sqlite,read_only=false \
-p 4444:4444 \
-p 4445:4445 \
-e SECRETS_SYSTEM=just-for-example-change-me \
-e DSN=sqlite:///var/lib/sqlite/db.sqlite?_fk=true \
-e URLS_SELF_ISSUER=https://localhost:5444/ \
-e URLS_CONSENT=http://localhost:9020/consent \
-e URLS_LOGIN=http://localhost:9020/login \
--entrypoint "migrate sql" \
oryd/hydra:latest-sqlite

docker-compose -f docker-compose.yml exec hydra \
hydra clients create \
--endpoint http://127.0.0.1:4445 \
--id auth-code-client \
--secret secret \
--grant-types authorization_code,refresh_token \
--response-types code,id_token \
--scope openid,offline,offline_access,profile \
--callbacks http://127.0.0.1:8080/client-callback


http://localhost:4444/oauth2/auth?response_type=code&client_id=auth-code-client&redirect_uri=http%3A%2F%2F127.0.0.1%3A8080%2Fclient-callback&scope=offline_access+openid+offline+profile&state=12345678901234567890

http://localhost:4444/oauth2/auth?response_type=code&client_id=auth-code-client&scope=offline_access+openid+offline+profile&state=12345678901234567890
