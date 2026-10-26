package com.example.pandasoft.assignment.service.data;

import com.example.pandasoft.assignment.entity.LeaveRequestLog;
import com.example.pandasoft.assignment.repository.LeaveRequestLogRepository;
import org.springframework.stereotype.Service;


@Service
public class LeaveRequestLogDataService {

    private final LeaveRequestLogRepository repository;

    public LeaveRequestLogDataService(LeaveRequestLogRepository repository) {
        this.repository = repository;
    }

    public LeaveRequestLog save(LeaveRequestLog log) {
        return repository.save(log);
    }

}
