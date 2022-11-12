package com.ardetrick.oryhydrareference.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
        value = HttpStatus.BAD_REQUEST,
        reason = "The query parameter login_challenge is required."
)
public class MissingLoginChallengeException extends HydraReferenceAppException {}
