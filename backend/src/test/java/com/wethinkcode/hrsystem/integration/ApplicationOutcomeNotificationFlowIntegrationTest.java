package com.wethinkcode.hrsystem.integration;

import com.wethinkcode.hrsystem.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ApplicationOutcomeNotificationFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void applicationOutcomeChange_publishesAndConsumesNotificationEvent() {
        RegisteredUser hrUser = registerAndLoginHr("it-hr-outcome-flow");
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.set("Authorization", "Bearer " + hrUser.token());

        Long jobPostingId = createJobPosting(authHeaders, hrUser.id());
        Long applicationId = submitApplication(jobPostingId);

        Map<String, Object> outcomeBody = new HashMap<>();
        outcomeBody.put("outcome", "ACCEPTED");
        outcomeBody.put("outcomeReason", "Strong candidate");

        HttpHeaders outcomeHeaders = new HttpHeaders();
        outcomeHeaders.set("Authorization", "Bearer " + hrUser.token());
        outcomeHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> outcomeResponse = restTemplate.exchange(
                baseUrl() + "/api/applications/" + applicationId + "/outcome",
                HttpMethod.PATCH,
                new HttpEntity<>(outcomeBody, outcomeHeaders),
                Map.class);
        assertThat(outcomeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Confirms the message was actually published with the right routing key,
        // bound to the notification queue, and consumed by NotificationConsumer -
        // this is the exact flow the earlier "*.changed" vs "#.changed" binding bug broke.
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(currentNotificationQueueDepth()).isZero());
    }

    private record RegisteredUser(String token, Long id) {}

    private RegisteredUser registerAndLoginHr(String username) {
        Long userId = createUser(username, "TestPass123!", "HR").getId();

        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword("TestPass123!");
        ResponseEntity<Map> loginResponse =
                restTemplate.postForEntity(baseUrl() + "/api/auth/login", login, Map.class);
        String token = (String) loginResponse.getBody().get("token");

        return new RegisteredUser(token, userId);
    }

    private Long createJobPosting(HttpHeaders authHeaders, Long postedById) {
        Map<String, Object> jobPosting = new HashMap<>();
        jobPosting.put("title", "QA Engineer");
        jobPosting.put("description", "Integration test job posting");
        jobPosting.put("requirements", "None");
        jobPosting.put("department", "Engineering");
        jobPosting.put("startDate", LocalDate.now().minusDays(1).toString());
        jobPosting.put("endDate", LocalDate.now().plusDays(30).toString());
        jobPosting.put("maxApplications", 10);
        jobPosting.put("requiredDocuments", List.of());
        jobPosting.put("postedById", postedById);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeaders.getFirst("Authorization"));
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/job-postings", HttpMethod.POST,
                new HttpEntity<>(jobPosting, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return Long.valueOf(response.getBody().get("id").toString());
    }

    private Long submitApplication(Long jobPostingId) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("candidateName", "Integration Test Candidate");
        // Blank on purpose: NotificationConsumer skips sending real email when
        // candidateEmail is blank, so this flow never touches the real SMTP server.
        form.add("candidateEmail", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/applications/" + jobPostingId, HttpMethod.POST,
                new HttpEntity<>(form, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return Long.valueOf(response.getBody().get("id").toString());
    }

    private int currentNotificationQueueDepth() throws Exception {
        String auth = Base64.getEncoder().encodeToString(
                (RABBITMQ.getAdminUsername() + ":" + RABBITMQ.getAdminPassword()).getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RABBITMQ.getHttpUrl() + "/api/queues/%2f/notification.queue"))
                .header("Authorization", "Basic " + auth)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        String body = response.body();
        int idx = body.indexOf("\"messages\":");
        if (idx == -1) {
            throw new AssertionError("Queue stats not yet available: " + body);
        }
        int start = idx + "\"messages\":".length();
        int end = start;
        while (end < body.length() && Character.isDigit(body.charAt(end))) {
            end++;
        }
        return Integer.parseInt(body.substring(start, end));
    }
}
