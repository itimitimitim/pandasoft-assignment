package com.example.pandasoft.assignment.repository;

import com.example.pandasoft.assignment.entity.LeaveRequest;
import com.example.pandasoft.assignment.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    @Query("""
            select r from LeaveRequest r
            where r.employeeId = :employeeId
              and r.status in :statuses
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    List<LeaveRequest> findConflicts(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<LeaveRequest.Status> statuses);

    @Query("""
            select r from LeaveRequest r
            where r.employeeId = :employeeId
              and r.leaveType = :type
              and r.status in :statuses
              and r.startDate <= :yearEnd
              and r.endDate >= :yearStart
            """)
    List<LeaveRequest> findCounted(
            @Param("employeeId") Long employeeId,
            @Param("type") LeaveType.Code type,
            @Param("yearStart") LocalDate yearStart,
            @Param("yearEnd") LocalDate yearEnd,
            @Param("statuses") Collection<LeaveRequest.Status> statuses);
}
