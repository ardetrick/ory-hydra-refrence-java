package com.ardetrick.oryhydrareference.hydra;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record AcceptLoginRequest(
    @NonNull String loginChallenge, @NonNull String subject, boolean remember) {}
