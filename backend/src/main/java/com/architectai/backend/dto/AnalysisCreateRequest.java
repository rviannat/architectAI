package com.architectai.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalysisCreateRequest(
    @NotBlank(message = "type is required")
    String type
) {}

