package com.meetmind.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when client input violates business or request constraints.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
