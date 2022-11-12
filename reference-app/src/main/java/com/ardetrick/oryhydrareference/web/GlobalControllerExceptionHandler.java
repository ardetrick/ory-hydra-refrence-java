package com.ardetrick.oryhydrareference.web;

import com.ardetrick.oryhydrareference.exception.AcceptLoginRequestRedirectException;
import com.ardetrick.oryhydrareference.exception.LoginRequestNotFoundException;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(AcceptLoginRequestRedirectException.class)
    public ResponseEntity<Void> handleAcceptLoginRequestRedirectException(AcceptLoginRequestRedirectException e) {
        val headers = new HttpHeaders();
        headers.add("Location", e.getRedirectLocation());
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

}
