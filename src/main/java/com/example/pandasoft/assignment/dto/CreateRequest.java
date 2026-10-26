package com.example.pandasoft.assignment.dto;

import com.example.pandasoft.assignment.entity.LeaveType;

import java.time.LocalDate;

public record CreateRequest(
        LeaveType.Code leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason) {
}
