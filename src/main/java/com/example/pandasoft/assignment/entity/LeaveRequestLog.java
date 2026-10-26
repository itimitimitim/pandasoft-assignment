package com.example.pandasoft.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "leave_request_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "leave_request_id", nullable = false)
    private Long leaveRequestId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private LeaveRequest.Status fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private LeaveRequest.Status toStatus;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    public LeaveRequestLog(
            Long leaveRequestId,
            Long actorId,
            LeaveRequest.Status fromStatus,
            LeaveRequest.Status toStatus) {
        this.leaveRequestId = leaveRequestId;
        this.actorId = actorId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedAt = Instant.now();
    }
}
