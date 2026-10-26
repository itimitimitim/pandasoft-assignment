package com.example.pandasoft.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    @Id
    @Column(length = 255)
    private String key;

    @Column(nullable = false)
    private Long employeeId;

    private Long leaveRequestId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public IdempotencyRecord(String key, Long employeeId) {
        this.key = key;
        this.employeeId = employeeId;
        this.createdAt = Instant.now();
    }
}
