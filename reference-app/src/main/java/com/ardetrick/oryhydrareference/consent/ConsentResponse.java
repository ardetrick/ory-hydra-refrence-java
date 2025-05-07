package com.ardetrick.oryhydrareference.consent;

import java.util.List;

public sealed interface ConsentResponse permits
        ConsentResponse.Accepted,
        ConsentResponse.DisplayUI,
        ConsentResponse.Skip {

    record Skip(String redirectTo) implements ConsentResponse {}

    record DisplayUI(List<String> requestedScopes,
                     String consentChallenge) implements ConsentResponse { }

    record Accepted(String redirectTo) implements ConsentResponse { }

}
