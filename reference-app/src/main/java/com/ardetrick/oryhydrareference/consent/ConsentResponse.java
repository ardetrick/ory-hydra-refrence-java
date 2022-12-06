package com.ardetrick.oryhydrareference.consent;

import java.util.List;

sealed interface ConsentResponse
        permits ConsentResponseAccepted, ConsentResponseRequiresUIDisplay, ConsentResponseSkip {
}

record ConsentResponseSkip(String redirectTo)
        implements ConsentResponse {}

record ConsentResponseRequiresUIDisplay(List<String> requestedScopes, String consentChallenge)
        implements ConsentResponse {}

record ConsentResponseAccepted(String redirectTo) implements ConsentResponse {}
