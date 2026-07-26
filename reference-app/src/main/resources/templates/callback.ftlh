<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Callback</title>
  <link rel="stylesheet" href="/pico.min.css">
</head>
<body>
<main class="container"><article>
<h1>Callback</h1>

<#if error??>
  <p>Hydra redirected back with an OAuth error instead of a code:</p>
  <pre>${error}<#if errorDescription??>: ${errorDescription}</#if></pre>
  <a href="/">Back to start</a>
<#else>
  <p>Hydra redirected back with an authorization code. Exchange it for tokens to finish the flow.</p>
  <p><small>In a real app your server makes this exchange automatically, without the browser ever
  seeing the code. Here you click it so you can watch the step happen.</small></p>

  <form action="/callback/exchange" method="post">
    <input type="hidden" name="code" value="${code!""}" />
    <input type="hidden" name="redirectUri" value="${redirectUri}" />

    <input type="submit" id="exchange" name="submit" value="Exchange code for tokens" />

    <details>
      <summary>View raw code</summary>
      <pre>${code!""}</pre>
    </details>

    <details>
      <summary>Client credentials</summary>
      <p><small>Pre-filled for the seeded demo client; change them if you started the flow with a
      different client.</small></p>
      <label for="clientId">Client ID</label>
      <input type="text" id="clientId" name="clientId" value="demo-client" />
      <label for="clientSecret">Client secret</label>
      <input type="text" id="clientSecret" name="clientSecret" value="omit-for-random-secret-1" />
    </details>
  </form>
</#if>
</article></main>
</body>
</html>
