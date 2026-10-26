package com.example.pandasoft.assignment.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class EmployeePrincipal implements UserDetails {

    private final Long employeeId;
    private final String role;

    public EmployeePrincipal(Long employeeId, String role) {
        this.employeeId = employeeId;
        this.role = role;
    }

    public Long employeeId() {
        return employeeId;
    }

    public String role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() { return ""; }

    @Override
    public String getUsername() { return employeeId.toString(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
