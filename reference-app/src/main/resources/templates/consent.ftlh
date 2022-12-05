<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
</head>
<body>
<form action="/consent" method="post">
  <!--<input type="hidden" name="csrf_token" value="...." />-->

  <input type="hidden" name="consentChallenge" value="${consentChallenge}" />

  <!-- for each scope being requested... -->
  <label for="scopes">Scopes</label><br>
  <input type="checkbox" id=scope name=scope checked/><br>

  <label for="remember">Remember me:</label><br>
  <input type="checkbox" id="remember" name="remember" checked />

  <input type="submit" id="accept" name="submit" value="Allow access" />
  <input type="submit" id="reject" name="submit" value="Deny access" />
</form>
</body>
</html>
