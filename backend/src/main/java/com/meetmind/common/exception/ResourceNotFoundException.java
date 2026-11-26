package com.meetmind.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested domain resource cannot be found.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
