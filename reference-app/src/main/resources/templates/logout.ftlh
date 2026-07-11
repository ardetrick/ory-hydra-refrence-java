<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
</head>
<body>
<h2>Log Out</h2>
<p>Do you want to log out ${subject}?</p>
<form action="/logout" method="post">
  <input type="hidden" name="logoutChallenge" value="${logoutChallenge}" />

  <input type="submit" id="confirm" name="confirm" value="Yes, log out" />
  <input type="submit" id="cancel" name="cancel" value="Cancel" />
</form>
</body>
</html>
