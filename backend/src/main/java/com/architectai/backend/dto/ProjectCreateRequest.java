package com.architectai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProjectCreateRequest(
    @NotBlank(message = "repoUrl is required")
    @Pattern(regexp = "^(https?://|git@).+", message = "repoUrl must be a valid git URL")
    String repoUrl,

    @NotBlank(message = "defaultBranch is required")
    String defaultBranch
) {}

