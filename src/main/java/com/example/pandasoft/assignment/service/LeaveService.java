package com.example.pandasoft.assignment.service;

import com.example.pandasoft.assignment.api.ApiException;
import com.example.pandasoft.assignment.entity.Employee;
import com.example.pandasoft.assignment.entity.LeaveRequest;
import com.example.pandasoft.assignment.entity.LeaveRequestLog;
import com.example.pandasoft.assignment.entity.LeaveType;
import com.example.pandasoft.assignment.event.LeaveApprovedEventPublisher;
import com.example.pandasoft.assignment.dto.CreateRequest;
import com.example.pandasoft.assignment.service.data.EmployeeDataService;
import com.example.pandasoft.assignment.service.data.LeaveRequestDataService;
import com.example.pandasoft.assignment.service.data.LeaveTypeDataService;
import com.example.pandasoft.assignment.service.data.LeaveRequestLogDataService;
import com.example.pandasoft.assignment.util.LeaveDateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class LeaveService {

    private static final List<LeaveRequest.Status> COUNTED_STATUSES = List.of(
            LeaveRequest.Status.PENDING,
            LeaveRequest.Status.APPROVED
    );

    private final LeaveRequestDataService requests;
    private final EmployeeDataService employees;
    private final LeaveTypeDataService types;
    private final IdempotencyService idempotency;
    private final LeaveRequestLogDataService logs;
    private final LeaveApprovedEventPublisher approvedEvents;
    private final Set<LocalDate> holidays;

    public LeaveService(
            LeaveRequestDataService requests,
            EmployeeDataService employees,
            LeaveTypeDataService types,
            IdempotencyService idempotency,
            LeaveRequestLogDataService logs,
            LeaveApprovedEventPublisher approvedEvents,
            @Value("${leave.holidays-2026:}") String holidayConfig) {
        this.requests = requests;
        this.employees = employees;
        this.types = types;
        this.idempotency = idempotency;
        this.logs = logs;
        this.approvedEvents = approvedEvents;
        this.holidays = LeaveDateUtils.parseHolidays(holidayConfig);
    }

    @Transactional
    public LeaveRequest create(Long actor, String idempotencyKey, CreateRequest body) {
        var existing = idempotency.reserveOrGet(idempotencyKey, actor);
        if (existing != null) {
            return requests.getRequired(existing.getLeaveRequestId());
        }

        Employee owner = employees.getRequired(actor);

        if (owner.getRole() != Employee.Role.EMPLOYEE) {
            throw ApiException.forbidden();
        }
        if (!types.exists(body.leaveType())) {
            throw ApiException.bad("Unknown leave type.");
        }

        LocalDate today = LocalDate.now();
        validateDates(body, today);

        int days = LeaveDateUtils.workingDays(body.startDate(), body.endDate(), holidays);
        if (days < 1) {
            throw ApiException.bad("The requested range must contain at least one working day.");
        }

        int year = body.startDate().getYear();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        int used = requests.findCounted(
                        actor, body.leaveType(), yearStart, yearEnd, COUNTED_STATUSES)
                .stream()
                .filter(request -> request.getStartDate().getYear() == year)
                .mapToInt(LeaveRequest::getDays)
                .sum();

        int quota = types.getRequired(body.leaveType()).getYearlyQuota();
        if (days + used > quota) {
            throw ApiException.conflict(
                    "INSUFFICIENT_BALANCE",
                    "Request exceeds the remaining " + body.leaveType()
                            + " quota of " + Math.max(0, quota - used)
                            + " days in " + year + ".");
        }

        if (overlapsExistingWorkingDay(actor, body.startDate(), body.endDate())) {
            throw ApiException.conflict(
                    "OVERLAPPING_REQUEST",
                    "This request overlaps a pending or approved request.");
        }

        LeaveRequest created = requests.saveAndFlush(new LeaveRequest(
                actor,
                body.leaveType(),
                body.startDate(),
                body.endDate(),
                body.reason(),
                days));
        logs.save(new LeaveRequestLog(
                created.getId(), actor, null, LeaveRequest.Status.PENDING));
        idempotency.complete(idempotencyKey, created.getId());
        return created;
    }

    private void validateDates(CreateRequest body, LocalDate today) {
        if (body.endDate().isBefore(body.startDate())) {
            throw ApiException.bad("endDate must be on or after startDate.");
        }
        if (body.leaveType() != LeaveType.Code.SICK
                && body.startDate().isBefore(today)) {
            throw ApiException.bad("ANNUAL and PERSONAL leave must start today or later.");
        }
        if (body.leaveType() == LeaveType.Code.SICK
                && body.startDate().isBefore(today.minusDays(7))) {
            throw ApiException.bad("SICK leave may be backdated by at most 7 days.");
        }
    }

    private boolean overlapsExistingWorkingDay(
            Long employeeId, LocalDate start, LocalDate end) {
        for (LeaveRequest existing : requests.findConflicts(
                employeeId, start, end, COUNTED_STATUSES)) {
            LocalDate overlapStart = existing.getStartDate().isAfter(start)
                    ? existing.getStartDate() : start;
            LocalDate overlapEnd = existing.getEndDate().isBefore(end)
                    ? existing.getEndDate() : end;
            if (LeaveDateUtils.workingDays(overlapStart, overlapEnd, holidays) > 0) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequest> list(
            Long actor,
            Long employeeId,
            LeaveRequest.Status status,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        Long target = employeeId == null ? actor : employeeId;
        employees.getRequired(actor);

        if (!target.equals(actor)
                && !Objects.equals(employees.getRequired(target).getManagerId(), actor)) {
            throw ApiException.forbidden();
        }

        return requests.findByFilters(target, status, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public LeaveRequest get(Long actor, Long id) {
        LeaveRequest request = request(id);
        employees.getRequired(actor);
        boolean owner = request.getEmployeeId().equals(actor);
        boolean manager = Objects.equals(
                employees.getRequired(request.getEmployeeId()).getManagerId(), actor);
        if (!owner && !manager) {
            throw ApiException.forbidden();
        }
        return request;
    }

    private LeaveRequest request(Long id) {
        return requests.getRequired(id);
    }

    @Transactional
    public LeaveRequest decide(Long actor, Long id, boolean approve, String reason) {
        Employee manager = employees.getRequired(actor);
        LeaveRequest request = request(id);
        boolean ownsApproval = Objects.equals(
                employees.getRequired(request.getEmployeeId()).getManagerId(), actor);
        if (manager.getRole() != Employee.Role.MANAGER || !ownsApproval
                || request.getEmployeeId().equals(actor)) {
            throw ApiException.forbidden();
        }
        if (request.getStatus() != LeaveRequest.Status.PENDING) {
            throw ApiException.conflict(
                    "INVALID_STATUS_TRANSITION",
                    "Only a PENDING request can be approved or rejected.");
        }
        LeaveRequest.Status fromStatus = request.getStatus();
        LeaveRequest.Status toStatus = approve
                ? LeaveRequest.Status.APPROVED
                : LeaveRequest.Status.REJECTED;
        request.decide(toStatus, actor, reason);
        LeaveRequest saved = requests.saveAndFlush(request);
        logs.save(new LeaveRequestLog(saved.getId(), actor, fromStatus, toStatus));
        if (approve) {
            approvedEvents.publish(saved);
        }
        return saved;
    }

    @Transactional
    public LeaveRequest cancel(Long actor, Long id) {
        LeaveRequest request = request(id);
        if (!request.getEmployeeId().equals(actor)) {
            throw ApiException.forbidden();
        }
        if (request.getStatus() != LeaveRequest.Status.PENDING
                && request.getStatus() != LeaveRequest.Status.APPROVED) {
            throw ApiException.conflict(
                    "INVALID_STATUS_TRANSITION",
                    "Only a PENDING or APPROVED request can be cancelled.");
        }
        if (!request.getStartDate().isAfter(LocalDate.now())) {
            throw ApiException.conflict(
                    "INVALID_STATUS_TRANSITION",
                    "A request can only be cancelled before its leave starts.");
        }
        LeaveRequest.Status fromStatus = request.getStatus();
        request.decide(LeaveRequest.Status.CANCELLED, actor, null);
        LeaveRequest saved = requests.saveAndFlush(request);
        logs.save(new LeaveRequestLog(
                saved.getId(), actor, fromStatus, LeaveRequest.Status.CANCELLED));
        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> balance(Long actor, Long id, int year) {
        employees.getRequired(actor);
        if (!actor.equals(id)
                && !Objects.equals(employees.getRequired(id).getManagerId(), actor)) {
            throw ApiException.forbidden();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeeId", id);
        result.put("year", year);

        Map<String, Map<String, Integer>> balances = new LinkedHashMap<>();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        for (LeaveType type : types.findAll()) {
            int used = requests.findCounted(
                            id, type.getCode(), yearStart, yearEnd, COUNTED_STATUSES)
                    .stream()
                    .mapToInt(request -> LeaveDateUtils.workingDays(
                            request.getStartDate().isBefore(yearStart) ? yearStart : request.getStartDate(),
                            request.getEndDate().isAfter(yearEnd) ? yearEnd : request.getEndDate(),
                            holidays))
                    .sum();
            balances.put(type.getCode().name(), Map.of(
                    "quota", type.getYearlyQuota(),
                    "used", used,
                    "remaining", Math.max(0, type.getYearlyQuota() - used)));
        }

        result.put("balances", balances);
        return result;
    }

}
