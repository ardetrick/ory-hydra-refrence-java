<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
</head>
<body>
<h2>Log In</h2>
<form action="/login/usernamePassword" method="post">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
  <input type="hidden" name="loginChallenge" value="${loginChallenge}" />

  <label for="loginEmail">Email:</label><br>
  <input type="email" name="loginEmail" placeholder="foo@bar.com" />
  <label for="loginPassword">Password:</label><br>
  <input type="password" name="loginPassword" placeholder="password" />

  <input type="submit" name="submit" value="Log in" />
</form>

<p>${(error)!}</p>

</body>
</html>
