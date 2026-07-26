<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Token Response</title>
  <link rel="stylesheet" href="/pico.min.css">
</head>
<body>
<main class="container"><article>
<h1>Token Response</h1>
<p>Success — Hydra exchanged the code for a real access token, ID token, and refresh token.</p>

<details>
  <summary>Raw token response</summary>
  <p><small>The raw JSON from Hydra's token endpoint.</small></p>
  <pre>${tokenResponse}</pre>
</details>

<#if idTokenClaims??>
<details>
  <summary>Decoded id_token claims</summary>
  <p><small>The payload of the id_token JWT (signature not verified here — that is the client's
  job, against <code>/.well-known/jwks.json</code>).</small></p>
  <pre>${idTokenClaims}</pre>
</details>
</#if>

<p><a href="/">Back to start</a></p>
</article></main>
</body>
</html>
