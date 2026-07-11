package com.ardetrick.oryhydrareference.logout;

/** The Java object representing the data submitted by the form in logout.ftlh. */
public record LogoutForm(String logoutChallenge, String cancel) {

  /**
   * The form has two submit buttons and only the one actually clicked is included in the form post,
   * so this field is non-null exactly when the user clicked "Cancel".
   */
  public boolean isCancelled() {
    return cancel != null;
  }
}
