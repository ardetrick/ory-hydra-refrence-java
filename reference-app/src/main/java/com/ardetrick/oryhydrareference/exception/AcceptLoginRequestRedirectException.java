package com.ardetrick.oryhydrareference.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AcceptLoginRequestRedirectException extends RuntimeException {

    String redirectLocation;

}
