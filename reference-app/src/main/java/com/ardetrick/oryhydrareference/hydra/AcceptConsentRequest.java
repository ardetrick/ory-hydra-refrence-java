package com.ardetrick.oryhydrareference.hydra;

import lombok.Builder;
import lombok.NonNull;

import java.util.List;

@Builder
public record AcceptConsentRequest(@NonNull String consentChallenge,
                            boolean remember,
                            List<String> grantAccessTokenAudience,
                            List<String> scopes
) {}
