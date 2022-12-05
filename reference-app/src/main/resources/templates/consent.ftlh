<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
</head>
<body>
<form action="/consent" method="post">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
  <input type="hidden" name="consentChallenge" value="${consentChallenge}" />

  <!-- for each scope being requested... -->
  Requested Scopes<br>

  <#list scopes as scope>
    <input type="checkbox" name="scopes" id="scopes-${scope}" value="${scope}" checked>
    <label for="${scope}">${scope}</label><br>
  </#list>

  <label for="remember">Remember me:</label>
  <input type="checkbox" id="remember" name="remember" checked /><br>

  <input type="submit" id="accept" name="submit" value="Allow access" />
  <input type="submit" id="reject" name="submit" value="Deny access" />
</form>
</body>
</html>
