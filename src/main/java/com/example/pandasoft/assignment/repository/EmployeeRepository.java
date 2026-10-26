package com.example.pandasoft.assignment.repository;

import com.example.pandasoft.assignment.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee,Long> {}
