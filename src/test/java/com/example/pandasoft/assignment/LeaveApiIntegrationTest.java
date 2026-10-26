package com.example.pandasoft.assignment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LeaveApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("leave")
            .withUsername("leave")
            .withPassword("leave");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate http;

    private HttpHeaders headers(long employeeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token(employeeId));
        headers.set("Idempotency-Key", java.util.UUID.randomUUID().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String token(long employeeId) {
        ResponseEntity<String> response = http.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(
                        "{\"employeeId\":" + employeeId + ",\"password\":\"password\"}",
                        jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().replaceAll("(?s).*\\\"accessToken\\\":\\\"([^\\\"]+).*", "$1");
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void createsListsAndReadsRequest() {
        String body = """
                {"leaveType":"ANNUAL","startDate":"2026-12-01",
                 "endDate":"2026-12-02","reason":"Family trip"}
                """;

        ResponseEntity<String> created = http.postForEntity(
                "/api/v1/leave-requests",
                new HttpEntity<>(body, headers(1)),
                String.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String id = created.getBody().replaceAll("(?s).*\\\"id\\\":(\\d+).*", "$1");

        ResponseEntity<String> one = http.exchange(
                "/api/v1/leave-requests/" + id,
                HttpMethod.GET,
                new HttpEntity<>(headers(1)),
                String.class);

        assertThat(one.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(http.exchange(
                "/api/v1/leave-requests",
                HttpMethod.GET,
                new HttpEntity<>(headers(1)),
                String.class).getBody()).contains("Family trip");
    }

    @Test
    void rejectsInvalidPayloadWithFieldErrors() {
        ResponseEntity<String> response = http.postForEntity(
                "/api/v1/leave-requests",
                new HttpEntity<>("{\"leaveType\":\"ANNUAL\",\"reason\":\"\"}", headers(1)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("fieldErrors");
    }

    @Test
    void managerCanApproveAndOwnerCannotApprove() {
        String body = """
                {"leaveType":"PERSONAL","startDate":"2026-11-10",
                 "endDate":"2026-11-10","reason":"Appointment"}
                """;
        String created = http.postForEntity(
                "/api/v1/leave-requests",
                new HttpEntity<>(body, headers(2)),
                String.class).getBody();
        String id = created.replaceAll("(?s).*\\\"id\\\":(\\d+).*", "$1");

        ResponseEntity<String> refused = http.postForEntity(
                "/api/v1/leave-requests/" + id + "/approve",
                new HttpEntity<>("{}", headers(2)),
                String.class);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> approved = http.postForEntity(
                "/api/v1/leave-requests/" + id + "/approve",
                new HttpEntity<>("{}", headers(100)),
                String.class);
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody()).contains("APPROVED");
    }
}
