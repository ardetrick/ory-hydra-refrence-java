package com.ardetrick.oryhydrareference.consent;

import java.util.List;

/**
 * The Java object representing the data submitted by the form in consent.ftlh.
 */
public record ConsentForm(
        String submit,
        String consentChallenge,
        Boolean remember,
        List<String> scopes
) {

    /**
     * This field is implemented in HTML as a checkbox. When a checkbox element is not checked in a form
     * it is not submitted in the form at all. This provides a useful null safe getter.
     */
    public boolean isRemember() {
        if (remember == null) {
            return false;
        }
        return remember;
    }

}
