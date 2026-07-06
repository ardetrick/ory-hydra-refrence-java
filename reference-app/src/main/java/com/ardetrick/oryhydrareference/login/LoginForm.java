package com.ardetrick.oryhydrareference.login;

public record LoginForm(
    String loginEmail, String loginPassword, String loginChallenge, Boolean remember) {

  /**
   * This field is implemented in HTML as a checkbox. When a checkbox element is not checked in a
   * form it is not submitted in the form at all. This provides a useful null safe getter.
   */
  public boolean isRemember() {
    if (remember == null) {
      return false;
    }
    return remember;
  }
}
