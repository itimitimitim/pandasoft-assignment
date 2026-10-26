package com.example.pandasoft.assignment.service;

import com.example.pandasoft.assignment.api.ApiException;
import com.example.pandasoft.assignment.entity.IdempotencyRecord;
import com.example.pandasoft.assignment.service.data.IdempotencyDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyDataService dataService;

    /**
     * Reserves a new key. Returns the existing record when this is a retry.
     */
    public IdempotencyRecord reserveOrGet(String key, Long employeeId) {
        validateKey(key);

        if (dataService.reserve(key, employeeId)) {
            return null;
        }

        IdempotencyRecord existing = dataService.get(key);
        if (existing == null || !existing.getEmployeeId().equals(employeeId)) {
            throw ApiException.conflict(
                    "IDEMPOTENCY_KEY_REUSED",
                    "This Idempotency-Key belongs to another employee.");
        }
        if (existing.getLeaveRequestId() == null) {
            throw ApiException.conflict(
                    "IDEMPOTENCY_IN_PROGRESS",
                    "The original request is still being processed.");
        }
        return existing;
    }

    public void complete(String key, Long leaveRequestId) {
        IdempotencyRecord record = dataService.get(key);
        if (record != null) {
            dataService.complete(record, leaveRequestId);
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 255) {
            throw ApiException.bad(
                    "Idempotency-Key must be a non-empty value of at most 255 characters.");
        }
    }
}
