package com.example.pandasoft.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveType {

    @Id
    @Enumerated(EnumType.STRING)
    private Code code;

    @Column(nullable = false)
    private int yearlyQuota;

    public enum Code {
        ANNUAL,
        SICK,
        PERSONAL
    }
}
