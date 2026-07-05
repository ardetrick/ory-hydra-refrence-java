package com.ardetrick.oryhydrareference.hydra;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;

@Builder
public record AcceptConsentRequest(
    @NonNull String consentChallenge,
    boolean remember,
    List<String> grantAccessTokenAudience,
    List<String> scopes) {}
