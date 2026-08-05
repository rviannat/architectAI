package com.architectai.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    Instant timestamp,
    int status,
    String message,
    T data
) {
    public static <T> ApiResponse<T> of(int status, String message, T data) {
        return new ApiResponse<>(Instant.now(), status, message, data);
    }
}

