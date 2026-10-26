package com.example.pandasoft.assignment.controller;

import com.example.pandasoft.assignment.api.ApiException;
import com.example.pandasoft.assignment.entity.Employee;
import com.example.pandasoft.assignment.dto.LoginRequest;
import com.example.pandasoft.assignment.dto.LoginResponse;
import com.example.pandasoft.assignment.security.JwtService;
import com.example.pandasoft.assignment.service.data.EmployeeDataService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final EmployeeDataService employees;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            EmployeeDataService employees,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Employee employee = employees.getRequired(request.employeeId());
        if (!passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new ApiException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Invalid employee ID or password.");
        }
        return new LoginResponse(jwtService.issue(employee), employee.getId(), employee.getRole());
    }

}
