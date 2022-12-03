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
  <input type="checkbox" id=scope value=scope name=scope />
  <input type="checkbox" id="remember" value=1 name="remember" />

  <!-- TODO: add terms of service link -->
  <!-- TODO: add policy link -->

  <input type="submit" id="accept" name="submit" value="Allow access" />
  <input type="submit" id="reject" name="submit" value="Deny access" />
</form>
</body>
</html>
