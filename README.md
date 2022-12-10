# Ory Hydra Reference Implementation - Java

This is an _unofficial_ reference implementation of the User Login and Consent flow of an 
[Ory Hydra](https://github.com/ory) OAuth 2.0 server written in Java with SpringBoot. This project demos some
key features/flows/integrations and the of OAuth 2.0 Authorization Code Grant flow. It is mean to be a foundation for
production implementations but it is not an exhaustive implementation nor is guaranteed to be secure, bug free, fully
tested, or production ready.

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

The functional tests for this project run along all other tests with the standard gradle command.

```
./gradlew test
```

The functional tests are unique because there is practically no mocking. This makes for a slightly more complicated
setup, but it allows us to reproduce scenarios in a context very similar to what would be seen in production, all they
way from interacting with the UI back to Ory Hydra.

1. Using `@SpringBootTest`, the application is started on a random port. Note that the application also configures two extra controllers to help facilitate testing.
2. A Playwright browser instance is created (this will be shared for all tests).
3. Before each test, a new Test Container instance of Ory Hydra is started. This instance of Ory Hydra is running with an in memory database.
4. A Hydra OAuth client is created.
5. The Playwright browser loads the `/oauth2/auth` endpoint with the client's information.
6. The Playwright api is used to interact with the UI just as a user would do.
7. Optionally, the code may be exchanged for the token response.
8. Repeat from Step 3 for each test.

The extra controllers created are not ideal but are useful for testing. One of them is a `ForwardingController` which
helps work around some networking challenges with a circular dependency in configuration between the application and Ory
Hydra. At start up, the application must be aware of the urls of Hydra and Hydra must be aware of the urls of
the application. In production, this would not be an issue because static urls should be used. But in a test context both
the application and Hydra are running on dynamic ports. The second controller is `ClientCallBackController` which 
provides a hook for the client call back. This allows us to verify that Hydra actually calls the client's callback url
and provides us access to the `code` value so that it can be exchanged for the token response.

Since the token flow of OAuth is inherently UI driven, it is imperative that the UI be the driver for the tests. To aid
with this the `Playwright` framework is used. Allows us to use a headless driver to load the UI and use HTML selectors
to interact with the loaded page just like a human would.

Due to the overhead of Test Containers, the tests are a bit slow and there is likely some optimization that could be
made so that the Ory Hydra containers are re-used across each test rather than recreated for each test.

#### Playwright

#### Test Containers

### Running With Local Ory Hydra

Running the application and Ory Hydra locally is a useful way to manually interact with the application. Use the
following commands to start Hydra, configure a client, and start the reference application. Note: this does require
Docker and `jq`.



```
# All commands should be run in the root of this project.
# It is expected Docker is running and jq is installed.

# Pull Hydra
docker pull oryd/hydra:v2.0.2

# Use the same docker-compose file used by the functional tests to start Hydra 
# with an in memory database and run the migration sccripts. 
docker-compose -f docker-compose.yml up --build

# Open a new terminal...

# Start the Spring app. 
./gradlew bootRun

# Create a client. Uses the Hydra container to access the Hydra CLI.
hydra_client=$(docker-compose -f docker-compose.yml exec hydra \
    hydra create client \
    --endpoint http://127.0.0.1:4445 \
    --grant-type authorization_code,refresh_token \
    --response-type code,id_token \
    --format json \
    --scope openid --scope offline \
    --redirect-uri http://127.0.0.1:5555/callback
)

# Put the client ID and client secret values into env variables for later use.
hydra_client_id=$(echo $hydra_client | jq -r '.client_id')
hydra_client_secret=$(echo $hydra_client | jq -r '.client_secret')
hydra_client_redirect_uri_0=$(echo $hydra_client | jq -r '.redirect_uris[0]')

# Update the client to avoid some issues with the Java SDK.
curl -X PATCH \
  http://localhost:4445/admin/clients/${hydra_client_id} \
  --data-binary @patch_client_body.json

# Open a new terminal.

# Build the endpoint to initiate the OAuth flow
oauth_endpoint="http://localhost:4444/oauth2/auth?\
client_id=${hydra_client_id}&\
response_type=code&\
redirect_uri=${client_redirect_uri_0}&\
scope=openid+offline&\
state=123456789"

# Print the endpoint.
echo $oauth_endpoint

# Click on the printed endpoint (or paste it into a browser).
# Complete the OAuth flow (by default the hard coded crentials are username: foo@bar.com password: password).

# replace '...' with the `code` query param in the call back.
code=... 

# Exchange the authorization code for an access token
curl -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "authorization: Basic $(echo -n "${hydra_client_id}:${hydra_client_secret}" | base64)" \
  -d "grant_type=authorization_code&code=${code}&redirect_uri=http%3A%2F%2F127.0.0.1%3A5555%2Fcallback&client_id=${code_client_id}" \
  http://127.0.0.1:4444/oauth2/token
```

## OAuth 2.0 Authorization Code Grant Flow

The OAuth 2.0 Authorization Code Grant flow is a common OAuth flow that allows a client to request access to a user's
resources on a resource server. In this flow, the client first redirects the user to the authorization server to 
authenticate and authorize the client's access to the user's resources. If the user grants access, the authorization
server sends an authorization code to the client, which the client can then exchange for an access token. The access
token can then be used to access the user's resources on the resource server.

```mermaid
sequenceDiagram
    participant User
    participant User-Agent
    participant OAuth Client (Server)
    participant Identity Provider
    participant Authorization Server
```

## Example Flows

### Full OAuth Flow With OIDC

### Use 'Remember Me' To Skip Consent Screen

## GitHub Actions

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

## Additional Notes
