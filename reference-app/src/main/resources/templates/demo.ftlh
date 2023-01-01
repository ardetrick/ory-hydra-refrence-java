<h1>Demo</h1>

This page serves as a short cut to kick start the demonstration process for different existing clients.

<ol>
    <li>Create a client if there are no viable ones listed below.</li>
    <li>Find the client and redirect uri you are interested in testing (see list below).</li>
    <li>Copy the partially completed curl command for token exchange (this will be used later). Note it assumes the default client secret, change as needed.</li>
    <li>Follow the link to and complete the OAuth flow.</li>
    <li>Get the "code" value from the query param.</li>
    <li>Place the code in the partially completed curl command.</li>
    <li>Observe the response! Consider inspecting the JWT with <a href"https://jwt.io/">jwt.io</a></li>
</ol>

<h2>All Clients</h2>

<#list clients as client>
    <div>
    Client Name: ${client.name()} </br>
    Client ID: ${client.id()} </br>
    OAuth start links by redirect URI:
    <ul>
    <#list client.redirectUris() as redirectUri>
        <li>
        <#assign oauthUrl="http://localhost:4444/oauth2/auth?client_id=${client.id()}&response_type=code&redirect_uri=${redirectUri}&scope=openid+offline&state=${.now?long?c}">
        ${redirectUri} </br>
        <a href="${oauthUrl}">${oauthUrl}</a>
<pre><code>curl -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "authorization: basic $(echo -n "${client.id()}:omit-for-random-secret-1" | base64)" \
  -d "grant_type=authorization_code" \
  -d "code=CODE_GOES_HERE" \
  -d "redirect_uri=${redirectUri?url('ISO-8859-1')}" \
  -d "client_id=${client.id()}" \
  http://127.0.0.1:4444/oauth2/token
</code></pre>
        </li>
    </#list>
    </ul>
    </div>
</#list>

<h2>Create A Client</h2>
<div>
To create a client, run these commands. Afterwards, reload this page to see the new client.

<pre>
<code>
hydra_client=$(docker-compose -f docker-compose.yml exec hydra \
    hydra create client \
    --endpoint http://127.0.0.1:4445 \
    --grant-type authorization_code,refresh_token \
    --response-type code,id_token \
    --format json \
    --secret omit-for-random-secret-1 \
    --scope openid --scope offline \
    --redirect-uri http://127.0.0.1:5555/callback
)

# Put the client ID and client secret values into env variables for later use.
hydra_client_id=$(echo $hydra_client | jq -r '.client_id')
hydra_client_secret=$(echo $hydra_client | jq -r '.client_secret')
hydra_client_redirect_uri_0=$(echo $hydra_client | jq -r '.redirect_uris[0]')

# Update the client to avoid some issues with the Java SDK.
curl -X PATCH \
  http://localhost:4445/admin/clients/$hydra_client_id \
  --data-binary @patch_client_body.json
</code>
</pre>

</div>
