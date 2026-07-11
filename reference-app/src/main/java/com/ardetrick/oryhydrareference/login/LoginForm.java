package com.ardetrick.oryhydrareference.login;

public record LoginForm(
    String loginEmail, String loginPassword, String loginChallenge, Boolean remember) {

  /**
   * Unchecked HTML checkboxes are omitted from the form post entirely — there is no {@code
   * remember=false} — so Spring binds this as {@code null} when the box is unchecked. The boxed
   * {@code Boolean} and this getter exist to absorb that.
   */
  public boolean isRemember() {
    if (remember == null) {
      return false;
    }
    return remember;
  }
}
