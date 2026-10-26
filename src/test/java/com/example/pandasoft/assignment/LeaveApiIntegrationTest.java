package com.example.pandasoft.assignment;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
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
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LeaveApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("leave")
            .withUsername("leave")
            .withPassword("leave");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.9.1"));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("kafka.enabled", () -> "true");
        registry.add("spring.kafka.admin.auto-create", () -> "true");
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

        try (KafkaConsumer<String, String> consumer = kafkaConsumer()) {
            consumer.subscribe(List.of("leave.approved"));

            ResponseEntity<String> approved = http.postForEntity(
                    "/api/v1/leave-requests/" + id + "/approve",
                    new HttpEntity<>("{}", headers(100)),
                    String.class);
            assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(approved.getBody()).contains("APPROVED");

            boolean eventReceived = false;
            long deadline = System.currentTimeMillis() + 15_000;
            while (!eventReceived && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (var record : records.records("leave.approved")) {
                    if (record.value().contains("\"leaveRequestId\":" + id)) {
                        eventReceived = true;
                        break;
                    }
                }
            }
            assertThat(eventReceived).isTrue();
        }
    }

    private KafkaConsumer<String, String> kafkaConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "leave-api-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
    }
}
