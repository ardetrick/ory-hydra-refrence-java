<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Ory Hydra Reference App</title>
</head>
<body>
<h1>Ory Hydra Reference App</h1>

<p>This app implements the login, consent, and logout screens for an
<a href="https://www.ory.sh/hydra">Ory Hydra</a> OAuth 2.0 server. Start a flow below, log in
with <code>foo@bar.com</code> / <code>password</code>, and you will end on a page that exchanges
the authorization code for real tokens.</p>

<h2>Start a flow</h2>

<#list clients as client>
  <div>
    <#if client.name()?has_content>${client.name()} — </#if><code>${client.id()}</code>
    <ul>
    <#list client.startLinks() as link>
      <li>
        <#if link.servedByThisApp()>
          <a data-testid="start-${client.id()}" href="${link.url()}">Start the OAuth flow</a>
        <#else>
          <a href="${link.url()}">Start the OAuth flow</a> — redirects to
          <code>${link.redirectUri()}</code>, which this app does not serve; copy the
          <code>code</code> query parameter from the browser afterwards.
        </#if>
      </li>
    </#list>
    </ul>
  </div>
<#else>
  <p>No OAuth2 clients are registered yet — expand the section below to create one.</p>
</#list>

<details>
<summary>Create another client</summary>
<div>
<p>Started via <code>./gradlew bootTestRun</code>? A demo client is already registered and listed
above. To create another client (the commands assume a Hydra container named <code>hydra</code>,
as in the README's "Running With Your Own Ory Hydra" instructions), run these commands and
reload this page.</p>

<pre>
<code>
hydra_client=$(docker exec hydra \
    hydra create client \
    --endpoint http://127.0.0.1:4445 \
    --grant-type authorization_code,refresh_token \
    --response-type code,id_token \
    --format json \
    --secret omit-for-random-secret-1 \
    --scope openid --scope offline \
    --redirect-uri http://localhost:8080/callback
)

# Put the client ID and client secret values into env variables for later use.
hydra_client_id=$(echo $hydra_client | jq -r '.client_id')
hydra_client_secret=$(echo $hydra_client | jq -r '.client_secret')

# Update the client to avoid some issues with the Java SDK.
curl -X PATCH \
  http://localhost:4445/admin/clients/$hydra_client_id \
  --data-binary @patch_client_body.json
</code>
</pre>
</div>
</details>
</body>
</html>
