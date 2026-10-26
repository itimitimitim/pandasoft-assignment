package com.example.pandasoft.assignment.service.data;

import com.example.pandasoft.assignment.api.ApiException;
import com.example.pandasoft.assignment.entity.LeaveType;
import com.example.pandasoft.assignment.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveTypeDataService {

    private final LeaveTypeRepository repository;

    public boolean exists(LeaveType.Code code) {
        return repository.existsById(code);
    }

    public LeaveType getRequired(LeaveType.Code code) {
        return repository.findById(code)
                .orElseThrow(() -> ApiException.missing("Leave type"));
    }

    public List<LeaveType> findAll() {
        return repository.findAll();
    }
}
