package com.example.pandasoft.assignment.service.data;

import com.example.pandasoft.assignment.api.ApiException;
import com.example.pandasoft.assignment.entity.Employee;
import com.example.pandasoft.assignment.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeDataService {

    private final EmployeeRepository repository;

    public Employee getRequired(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.missing("Employee"));
    }
}
