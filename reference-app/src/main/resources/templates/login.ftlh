<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Log In</title>
  <link rel="stylesheet" href="/pico.min.css">
</head>
<body>
<main class="container"><article>
  <h1>Log In</h1>
  <p><small>Demo credentials are pre-filled. Select Log in to continue.</small></p>
  <form action="/login/usernamePassword" method="post">
    <input type="hidden" name="loginChallenge" value="${loginChallenge}" />

    <label for="loginEmail">Email</label>
    <input type="email" id="loginEmail" name="loginEmail" value="foo@bar.com" />

    <label for="loginPassword">Password</label>
    <input type="password" id="loginPassword" name="loginPassword" value="password" />

    <label>
      <input type="checkbox" id="remember" name="remember" />
      Remember me
    </label>

    <input type="submit" name="submit" value="Log in" />
  </form>

  <#if (error)?has_content>
    <p>${error}</p>
  </#if>
</article></main>
</body>
</html>
