<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
</head>
<body>
<form action="/consent" method="post">
  <!-- TODO: implement csrf protection -->
  <!--<input type="hidden" name="csrf_token" value="...." />-->
  <input type="hidden" name="consentChallenge" value="${consentChallenge}" />

  <!-- for each scope being requested... -->
  <label for="scopes">Scopes</label><br>

  <input type="checkbox" id=scope name=scope checked/>
  <label for="vehicle1">Example scope selection (make this dynamic)</label><br>

  <br>

  <input type="checkbox" id="remember" name="remember" checked />
  <label for="remember">Remember me</label><br>

  <!-- TODO: add terms of service link -->
  <!-- TODO: add policy link -->

  <input type="submit" id="accept" name="submit" value="Allow access" />
  <input type="submit" id="reject" name="submit" value="Deny access" />
</form>
</body>
</html>
