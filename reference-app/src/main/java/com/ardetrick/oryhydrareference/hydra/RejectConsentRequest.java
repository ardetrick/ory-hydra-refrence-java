package com.ardetrick.oryhydrareference.hydra;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record RejectConsentRequest(
    @NonNull String consentChallenge, @NonNull String error, String errorDescription) {}
