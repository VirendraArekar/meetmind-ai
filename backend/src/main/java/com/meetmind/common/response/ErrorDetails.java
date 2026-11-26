package com.meetmind.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Structured error details returned inside {@link ApiResponse} on failures.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {

    private final String code;
    private final String path;
    private final Map<String, String> fieldErrors;
}
