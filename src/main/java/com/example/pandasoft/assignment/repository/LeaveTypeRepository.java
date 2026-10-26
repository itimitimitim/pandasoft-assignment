package com.example.pandasoft.assignment.repository;

import com.example.pandasoft.assignment.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType,LeaveType.Code> {}
