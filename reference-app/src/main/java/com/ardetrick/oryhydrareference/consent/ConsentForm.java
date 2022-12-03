package com.ardetrick.oryhydrareference.consent;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsentForm {

    String submit;
    String consentChallenge;

}
