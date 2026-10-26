package com.example.pandasoft.assignment.dto;

import com.example.pandasoft.assignment.entity.Employee;

public record LoginResponse(
        String accessToken,
        Long employeeId,
        Employee.Role role) {
}
