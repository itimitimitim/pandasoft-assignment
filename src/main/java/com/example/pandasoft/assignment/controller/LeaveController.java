package com.example.pandasoft.assignment.controller;

import com.example.pandasoft.assignment.api.ApiException;
import com.example.pandasoft.assignment.entity.LeaveRequest;
import com.example.pandasoft.assignment.entity.LeaveType;
import com.example.pandasoft.assignment.dto.CreateRequest;
import com.example.pandasoft.assignment.security.EmployeePrincipal;
import com.example.pandasoft.assignment.service.LeaveService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@Validated
@RequestMapping("/api/v1")
public class LeaveController {

    private final LeaveService service;

    public LeaveController(LeaveService service) {
        this.service = service;
    }

    @PostMapping("/leave-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('EMPLOYEE')")
    public LeaveRequest create(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateBody body) {
        return service.create(principal.employeeId(), idempotencyKey, new CreateRequest(
                body.leaveType, body.startDate, body.endDate, body.reason));
    }

    @GetMapping("/leave-requests")
    public Page<LeaveRequest> list(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LeaveRequest.Status status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        if (from != null && to != null && to.isBefore(from)) {
            throw ApiException.bad("to must be on or after from.");
        }
        return service.list(principal.employeeId(), employeeId, status, from, to, pageable);
    }

    @GetMapping("/leave-requests/{id}")
    public LeaveRequest get(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @PathVariable Long id) {
        return service.get(principal.employeeId(), id);
    }

    @PostMapping("/leave-requests/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public LeaveRequest approve(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @PathVariable Long id) {
        return service.decide(principal.employeeId(), id, true, null);
    }

    @PostMapping("/leave-requests/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public LeaveRequest reject(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RejectBody body) {
        return service.decide(principal.employeeId(), id, false, body.reason);
    }

    @PostMapping("/leave-requests/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public LeaveRequest cancel(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @PathVariable Long id) {
        return service.cancel(principal.employeeId(), id);
    }

    @GetMapping("/employees/{id}/leave-balance")
    public Object balance(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @PathVariable Long id,
            @RequestParam(defaultValue = "2026") @Min(2000) @Max(2100) int year) {
        return service.balance(principal.employeeId(), id, year);
    }

    public static class CreateBody {
        @NotNull
        public LeaveType.Code leaveType;

        @NotNull
        public LocalDate startDate;

        @NotNull
        public LocalDate endDate;

        @NotBlank
        @Size(max = 1000)
        public String reason;
    }

    public static class RejectBody {
        @NotBlank
        @Size(max = 1000)
        public String reason;
    }
}
