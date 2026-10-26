package com.example.pandasoft.assignment.service;

import com.example.pandasoft.assignment.api.ApiException;
import com.example.pandasoft.assignment.entity.Employee;
import com.example.pandasoft.assignment.entity.LeaveRequest;
import com.example.pandasoft.assignment.entity.LeaveType;
import com.example.pandasoft.assignment.dto.CreateRequest;
import com.example.pandasoft.assignment.util.LeaveDateUtils;
import com.example.pandasoft.assignment.service.data.EmployeeDataService;
import com.example.pandasoft.assignment.service.data.LeaveRequestDataService;
import com.example.pandasoft.assignment.service.data.LeaveTypeDataService;
import com.example.pandasoft.assignment.service.data.LeaveRequestLogDataService;
import com.example.pandasoft.assignment.event.LeaveApprovedEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class LeaveRulesTest {

    @Mock
    LeaveRequestDataService requests;

    @Mock
    EmployeeDataService employees;

    @Mock
    LeaveTypeDataService types;

    @Mock
    IdempotencyService idempotency;

    @Mock
    LeaveRequestLogDataService logs;

    @Mock
    LeaveApprovedEventPublisher approvedEvents;

    private LeaveService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(idempotency.reserveOrGet(any(), anyLong())).thenReturn(null);
        when(employees.getRequiredForUpdate(anyLong()))
                .thenAnswer(invocation -> employees.getRequired(invocation.getArgument(0)));
        service = new LeaveService(
                requests, employees, types, idempotency, logs, approvedEvents, "2026-01-01");
    }

    @Test
    void countsWeekdaysAndExcludesConfiguredHoliday() {
        assertThat(LeaveDateUtils.workingDays(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 5),
                LeaveDateUtils.parseHolidays("2026-01-01"))).isEqualTo(2);
    }

    @Test
    void refusesRequestWhenRemainingBalanceIsTooSmall() {
        when(employees.getRequired(1L)).thenReturn(employee(1L));
        when(types.exists(LeaveType.Code.ANNUAL)).thenReturn(true);
        when(types.getRequired(LeaveType.Code.ANNUAL))
                .thenReturn(new LeaveType(LeaveType.Code.ANNUAL, 1));
        LocalDate start = weekday();
        when(requests.findCounted(
                eq(1L), eq(LeaveType.Code.ANNUAL), any(), any(), anyCollection()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.create(1L, "balance-test", new CreateRequest(
                LeaveType.Code.ANNUAL, start, start.plusDays(1), "Trip")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("remaining");
    }

    @Test
    void refusesOverlappingWorkingDays() {
        when(employees.getRequired(1L)).thenReturn(employee(1L));
        when(types.exists(LeaveType.Code.ANNUAL)).thenReturn(true);
        when(types.getRequired(LeaveType.Code.ANNUAL))
                .thenReturn(new LeaveType(LeaveType.Code.ANNUAL, 10));
        LocalDate start = weekday();
        when(requests.findCounted(
                eq(1L), eq(LeaveType.Code.ANNUAL), any(), any(), anyCollection()))
                .thenReturn(List.of());
        when(requests.findConflicts(eq(1L), any(), any(), anyCollection()))
                .thenReturn(List.of(new LeaveRequest(
                        1L, LeaveType.Code.PERSONAL, start, start, "Other", 1)));

        assertThatThrownBy(() -> service.create(1L, "overlap-test", new CreateRequest(
                LeaveType.Code.ANNUAL, start, start, "Trip")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void rejectsInvalidStatusTransition() {
        Employee manager = new Employee(
                100L, "Manager", "m@example.com", Employee.Role.MANAGER, null);
        LeaveRequest request = new LeaveRequest(
                1L,
                LeaveType.Code.ANNUAL,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(5),
                "Trip",
                1);
        request.decide(LeaveRequest.Status.REJECTED, 100L, "No");

        when(employees.getRequired(100L)).thenReturn(manager);
        when(employees.getRequired(1L)).thenReturn(employee(1L));
        when(requests.getRequired(4L)).thenReturn(request);

        assertThatThrownBy(() -> service.decide(100L, 4L, true, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("PENDING");
    }

    private Employee employee(long id) {
        return new Employee(id, "Employee", "employee@example.com", Employee.Role.EMPLOYEE, 100L);
    }

    private LocalDate weekday() {
        LocalDate date = LocalDate.now().plusDays(14);
        while (date.getDayOfWeek().getValue() > 5) {
            date = date.plusDays(1);
        }
        return date;
    }
}
