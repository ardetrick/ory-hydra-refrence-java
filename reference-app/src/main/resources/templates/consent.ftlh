<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Authorize access</title>
  <link rel="stylesheet" href="/pico.min.css">
</head>
<body>
<main class="container"><article>
  <h1>Authorize access</h1>
  <p><strong>${clientName}</strong> wants to access your account. Choose what to allow.</p>
  <form action="/consent" method="post">
    <input type="hidden" name="consentChallenge" value="${consentChallenge}" />

    <fieldset>
      <legend>This will let it:</legend>
      <#list scopes as scope>
        <label>
          <input type="checkbox" name="scopes" id="scopes-${scope.name()}" value="${scope.name()}" checked>
          <span><#if scope.description()?has_content>${scope.description()} <small>(${scope.name()})</small><#else>${scope.name()}</#if></span>
        </label>
      </#list>
    </fieldset>

    <label>
      <input type="checkbox" id="remember" name="remember" checked />
      Remember my decision
    </label>

    <div role="group">
      <input type="submit" id="accept" name="accept" value="Allow access" />
      <input type="submit" id="reject" name="deny" value="Deny access" class="secondary" />
    </div>
  </form>
</article></main>
</body>
</html>
