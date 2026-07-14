# Walking The Authorization Code Flow By Hand

The app's [landing page](http://localhost:8080) automates most of this, but doing the
exchange manually with curl shows exactly what crosses the wire at each step of the
authorization code grant. This walkthrough assumes a Hydra container named `hydra` (see
"Running With Your Own Ory Hydra" in the [README](../README.md)), the app running via
`./gradlew bootRun`, and `jq` installed. Run the commands from the repository root.

```
# Create a client. Uses the Hydra container to access the Hydra CLI.
hydra_client=$(docker exec hydra \
    hydra create client \
    --endpoint http://127.0.0.1:4445 \
    --grant-type authorization_code,refresh_token \
    --response-type code,id_token \
    --format json \
    --secret omit-for-random-secret-1 \
    --scope openid --scope offline \
    --redirect-uri http://127.0.0.1:5555/callback
)

# Put the client ID, client secret, and redirect URI into env variables for later use.
hydra_client_id=$(echo $hydra_client | jq -r '.client_id')
hydra_client_secret=$(echo $hydra_client | jq -r '.client_secret')
hydra_client_redirect_uri_0=$(echo $hydra_client | jq -r '.redirect_uris[0]')

# Update the client to avoid some deserialization issues in the Ory Java SDK the app uses
# to list clients on /demo.
curl -X PATCH \
  http://localhost:4445/admin/clients/$hydra_client_id \
  --data-binary @patch_client_body.json

# Build the endpoint that initiates the OAuth flow.
oauth_endpoint="http://localhost:4444/oauth2/auth?\
client_id=${hydra_client_id}&\
response_type=code&\
redirect_uri=${hydra_client_redirect_uri_0}&\
scope=openid+offline&\
state=123456789"

# Print the endpoint.
echo $oauth_endpoint

# Paste the printed endpoint into a browser and complete the OAuth flow (the hard coded
# credentials are username: foo@bar.com password: password). Nothing listens on the
# redirect URI, so the final redirect shows a connection error — that's expected. Copy the
# `code` query parameter out of the browser's address bar.
code=...

# Exchange the authorization code for the token response. Note the client authenticates
# with HTTP basic auth (client_id:client_secret, base64-encoded).
curl -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "authorization: basic $(echo -n "${hydra_client_id}:${hydra_client_secret}" | base64)" \
  -d "grant_type=authorization_code" \
  -d "code=${code}" \
  -d "redirect_uri=http%3A%2F%2F127.0.0.1%3A5555%2Fcallback" \
  -d "client_id=${hydra_client_id}" \
  http://127.0.0.1:4444/oauth2/token
```

The response contains the access token, a refresh token (because the `offline` scope was
granted), and an `id_token` JWT (because `openid` was granted). The README's OpenID Connect
section shows an example of decoding and verifying the id_token.
