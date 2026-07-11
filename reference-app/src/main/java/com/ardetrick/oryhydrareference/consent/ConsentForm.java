package com.ardetrick.oryhydrareference.consent;

import java.util.List;

/** The Java object representing the data submitted by the form in consent.ftlh. */
public record ConsentForm(
    String consentChallenge, Boolean remember, List<String> scopes, String deny) {

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

  /**
   * The form has two submit buttons and only the one actually clicked is included in the form post,
   * so this field is non-null exactly when the user clicked "Deny access".
   */
  public boolean isDenied() {
    return deny != null;
  }
}
