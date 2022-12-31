package com.ardetrick.oryhydrareference.web;

import com.ardetrick.oryhydrareference.exception.LoginRequestNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalControllerExceptionHandler {

    @ResponseStatus(
            value = HttpStatus.UNAUTHORIZED,
            reason = "The query parameter login_challenge is required."
    )
    @ExceptionHandler(LoginRequestNotFoundException.class)
    public void handleLoginRequestNotFoundException() {}

}
