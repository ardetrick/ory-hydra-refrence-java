package com.ardetrick.oryhydrareference.login;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginForm {
    String loginEmail;
    String loginPassword;
    String loginChallenge;
}
