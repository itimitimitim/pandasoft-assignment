package com.example.pandasoft.assignment.repository;

import com.example.pandasoft.assignment.entity.LeaveRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestLogRepository extends JpaRepository<LeaveRequestLog, Long> {
}
