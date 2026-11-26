package com.meetmind.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base domain exception carrying an HTTP status and machine-readable error code.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
