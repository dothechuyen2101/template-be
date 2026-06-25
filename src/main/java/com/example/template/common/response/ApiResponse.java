package com.example.template.common.response;

import com.example.template.common.constants.ApiMessages;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        List<FieldErrorResponse> errors,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return success(ApiMessages.OK, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    public static ApiResponse<Void> failure(String message) {
        return failure(message, null);
    }

    public static ApiResponse<Void> failure(String message, List<FieldErrorResponse> errors) {
        return new ApiResponse<>(false, message, null, errors, Instant.now());
    }
}
