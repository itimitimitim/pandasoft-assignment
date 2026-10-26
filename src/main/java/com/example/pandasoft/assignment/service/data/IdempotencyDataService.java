package com.example.pandasoft.assignment.service.data;

import com.example.pandasoft.assignment.entity.IdempotencyRecord;
import com.example.pandasoft.assignment.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdempotencyDataService {

    private final IdempotencyRepository repository;

    @Transactional
    public boolean reserve(String key, Long employeeId) {
        return repository.reserve(key, employeeId) == 1;
    }

    public IdempotencyRecord get(String key) {
        return repository.findById(key).orElse(null);
    }

    public void complete(IdempotencyRecord record, Long leaveRequestId) {
        record.setLeaveRequestId(leaveRequestId);
        repository.save(record);
    }
}
