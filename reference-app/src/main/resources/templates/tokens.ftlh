<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Token Response</title>
</head>
<body>
<h1>Token Response</h1>

<p>The raw response from Hydra's token endpoint:</p>
<pre>${tokenResponse}</pre>

<#if idTokenClaims??>
  <h2>Decoded id_token claims</h2>
  <p>The payload of the id_token JWT (signature not verified here — that is the client's job,
  against <code>/.well-known/jwks.json</code>):</p>
  <pre>${idTokenClaims}</pre>
</#if>

<a href="/">Back to start</a>
</body>
</html>
