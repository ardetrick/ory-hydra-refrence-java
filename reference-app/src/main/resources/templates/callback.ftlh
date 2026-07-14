<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Callback</title>
</head>
<body>
<h1>Callback</h1>

<#if error??>
  <p>Hydra redirected back with an OAuth error instead of a code:</p>
  <pre>${error}<#if errorDescription??>: ${errorDescription}</#if></pre>
  <a href="/">Back to start</a>
<#else>
  <p>Hydra redirected back with an authorization code:</p>
  <pre>${code!""}</pre>

  <p>Exchange it for tokens. The client fields are pre-filled for the seeded demo client; change
  them if you started the flow with a different client.</p>

  <form action="/callback/exchange" method="post">
    <input type="hidden" name="code" value="${code!""}" />
    <input type="hidden" name="redirectUri" value="${redirectUri}" />

    <label for="clientId">Client ID:</label>
    <input type="text" id="clientId" name="clientId" value="demo-client" size="40" /><br>
    <label for="clientSecret">Client secret:</label>
    <input type="text" id="clientSecret" name="clientSecret" value="omit-for-random-secret-1" size="40" /><br>

    <input type="submit" id="exchange" name="submit" value="Exchange code for tokens" />
  </form>
</#if>
</body>
</html>
