package com.example.pandasoft.assignment.service.data;

import com.example.pandasoft.assignment.api.ApiException;
import com.example.pandasoft.assignment.entity.LeaveRequest;
import com.example.pandasoft.assignment.entity.LeaveType;
import com.example.pandasoft.assignment.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestDataService {

    private final LeaveRequestRepository repository;

    public LeaveRequest getRequired(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.missing("Leave request"));
    }

    public LeaveRequest save(LeaveRequest request) {
        return repository.save(request);
    }

    public LeaveRequest saveAndFlush(LeaveRequest request) {
        return repository.saveAndFlush(request);
    }

    public Page<LeaveRequest> findByFilters(
            Long employeeId,
            LeaveRequest.Status status,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        Specification<LeaveRequest> specification = (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("employeeId"), employeeId));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), to));
            }

            return criteriaBuilder.and(
                    predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return repository.findAll(specification, pageable);
    }

    public List<LeaveRequest> findConflicts(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            Collection<LeaveRequest.Status> statuses) {
        return repository.findConflicts(employeeId, startDate, endDate, statuses);
    }

    public List<LeaveRequest> findCounted(
            Long employeeId,
            LeaveType.Code leaveType,
            LocalDate yearStart,
            LocalDate yearEnd,
            Collection<LeaveRequest.Status> statuses) {
        return repository.findCounted(employeeId, leaveType, yearStart, yearEnd, statuses);
    }
}
