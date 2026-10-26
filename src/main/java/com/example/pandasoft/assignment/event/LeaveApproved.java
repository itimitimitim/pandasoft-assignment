package com.example.pandasoft.assignment.event;

import com.example.pandasoft.assignment.entity.LeaveType;

public record LeaveApproved(
        Long leaveRequestId,
        Long employeeId,
        LeaveType.Code leaveType,
        String startDate,
        String endDate,
        int days,
        Long approvedBy,
        String approvedAt) {
}
