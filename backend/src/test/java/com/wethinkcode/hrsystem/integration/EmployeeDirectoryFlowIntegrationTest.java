package com.wethinkcode.hrsystem.integration;

import com.wethinkcode.hrsystem.dto.LoginRequest;
import com.wethinkcode.hrsystem.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDirectoryFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void employeeCreationWithBranchAndManager_reflectsCorrectlyInDirectory() {
        HttpHeaders headers = registerAndLoginHr("it-hr-directory-flow");

        Long branchId = createBranch(headers);
        Long managerId = createEmployee(headers, "Dana Manager", null, branchId, null);
        Long reportId = createEmployee(headers, "Riley Report", "Engineer", branchId, managerId);

        ResponseEntity<List> directoryResponse = restTemplate.exchange(
                baseUrl() + "/api/employee-directory?branchId=" + branchId,
                HttpMethod.GET, new HttpEntity<>(headers), List.class);
        assertThat(directoryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String, Object>> entries = directoryResponse.getBody();
        assertThat(entries).hasSize(2);

        Map<String, Object> reportEntry = entries.stream()
                .filter(e -> e.get("id").toString().equals(reportId.toString()))
                .findFirst()
                .orElseThrow();

        // Confirms the real FK relationships (employees.branch_id -> branches,
        // employees.reports_to_id -> employees self-reference) resolved correctly
        // through actual JPA mappings against a real MySQL instance, not mocks.
        assertThat(reportEntry.get("branchName")).isEqualTo("Integration Test Branch");
        assertThat(reportEntry.get("reportsToName")).isEqualTo("Dana Manager");

        ResponseEntity<List> orgChartResponse = restTemplate.exchange(
                baseUrl() + "/api/employee-directory/org-chart",
                HttpMethod.GET, new HttpEntity<>(headers), List.class);
        assertThat(orgChartResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orgChartResponse.getBody()).isNotEmpty();
    }

    private HttpHeaders registerAndLoginHr(String username) {
        RegisterRequest register = new RegisterRequest();
        register.setUsername(username);
        register.setPassword("TestPass123!");
        register.setRole("HR");
        restTemplate.postForEntity(baseUrl() + "/api/auth/register", register, Map.class);

        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword("TestPass123!");
        ResponseEntity<Map> loginResponse =
                restTemplate.postForEntity(baseUrl() + "/api/auth/login", login, Map.class);
        String token = (String) loginResponse.getBody().get("token");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Long createBranch(HttpHeaders headers) {
        Map<String, Object> branch = new HashMap<>();
        branch.put("name", "Integration Test Branch");
        branch.put("address", "1 Test Street");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/branches", HttpMethod.POST,
                new HttpEntity<>(branch, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return Long.valueOf(response.getBody().get("id").toString());
    }

    private Long createEmployee(HttpHeaders headers, String fullName, String position, Long branchId, Long reportsToId) {
        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeNumber", "IT-" + UUID.randomUUID().toString().substring(0, 8));
        employee.put("fullName", fullName);
        employee.put("position", position);
        employee.put("department", "Engineering");
        employee.put("qualifications", "N/A");
        employee.put("contactDetails", "test@example.com");
        employee.put("employmentDate", LocalDate.now().toString());
        employee.put("branchId", branchId);
        employee.put("reportsToId", reportsToId);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/employees", HttpMethod.POST,
                new HttpEntity<>(employee, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return Long.valueOf(response.getBody().get("id").toString());
    }
}
