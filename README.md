# Ory Hydra Reference Implementation - Java

## Task List

- [ ] Add documentation explain application with screen shots
- [ ] Add a fancier UI
- [ ] Clean up integration tests
- [ ] Add custom OIDC claims
- [ ] Add more unit tests
- [ ] Document GitHub actions
- [ ] Document OIDC usage
- [ ] Document playwright usage
- [ ] Add CSRF
- [ ] Show login errors on login screen
- [ ] Add playwright traces https://playwright.dev/java/docs/trace-viewer-intro

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