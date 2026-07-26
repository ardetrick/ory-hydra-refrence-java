<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Log Out</title>
  <link rel="stylesheet" href="/pico.min.css">
</head>
<body>
<main class="container"><article>
  <h1>Log Out</h1>
  <p>Do you want to log out ${subject}?</p>
  <form action="/logout" method="post">
    <input type="hidden" name="logoutChallenge" value="${logoutChallenge}" />

    <div role="group">
      <input type="submit" id="confirm" name="confirm" value="Yes, log out" />
      <input type="submit" id="cancel" name="cancel" value="Cancel" class="secondary" />
    </div>
  </form>
</article></main>
</body>
</html>
