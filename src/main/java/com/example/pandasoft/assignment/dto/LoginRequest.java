package com.example.pandasoft.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull Long employeeId,
        @NotBlank String password) {
}
