<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
</head>
<body>
<h2>Log In</h2>
<form action="/login/usernamePassword" method="post">
  <!-- TODO: implement csrf protection -->
  <!--<input type="hidden" name="csrf_token" value="...." />-->
  <input type="hidden" name="loginChallenge" value="${loginChallenge}" />
  <input type="email" name="loginEmail" placeholder="foo@bar.com" />
  <input type="password" name="loginPassword" placeholder="password" />
  <input type="submit" name="submit" value="Log in" />

  <!-- TODO show error messages in a better way -->
  ${(error)!}
</form>
</body>
</html>
