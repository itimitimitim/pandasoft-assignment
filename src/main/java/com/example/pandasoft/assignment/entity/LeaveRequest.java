package com.example.pandasoft.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests", indexes = {
        @Index(name = "idx_leave_employee_dates", columnList = "employee_id,start_date,end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType.Code leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int days;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Long decidedBy;
    private Instant decidedAt;

    @Column(length = 1000)
    private String decisionReason;

    @Version
    private long version;

    public LeaveRequest(
            Long employeeId,
            LeaveType.Code leaveType,
            LocalDate startDate,
            LocalDate endDate,
            String reason,
            int days) {
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.days = days;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public void decide(Status status, Long actor, String reason) {
        this.status = status;
        this.decidedBy = actor;
        this.decidedAt = Instant.now();
        this.decisionReason = reason;
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }
}
