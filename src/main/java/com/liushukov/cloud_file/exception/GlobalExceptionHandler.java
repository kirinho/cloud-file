package com.liushukov.cloud_file.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception exception, WebRequest webRequest) {

        if (exception instanceof BadCredentialsException) {
            ErrorDetails errorDetails = new ErrorDetails(
                    Instant.now(),
                    "The username or password is incorrect",
                    webRequest.getDescription(false)
            );
            return ResponseEntity.status(HttpStatusCode.valueOf(401)).body(errorDetails);
        }
        return ResponseEntity.status(HttpStatusCode.valueOf(500))
                .body(new ErrorDetails(
                        Instant.now(),
                        "Internal server error",
                        webRequest.getDescription(false)
                ));
    }
}
