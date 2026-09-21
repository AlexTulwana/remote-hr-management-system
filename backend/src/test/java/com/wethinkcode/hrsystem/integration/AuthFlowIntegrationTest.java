package com.wethinkcode.hrsystem.integration;

import com.wethinkcode.hrsystem.dto.LoginRequest;
import com.wethinkcode.hrsystem.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    private String loginAndGetToken(String username) {
        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword("TestPass123!");
        ResponseEntity<Map> loginResponse =
                restTemplate.postForEntity(baseUrl() + "/api/auth/login", login, Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) loginResponse.getBody().get("token");
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    private ResponseEntity<Map> register(String token, String username, String role) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword("TestPass123!");
        request.setRole(role);
        HttpHeaders headers = token == null ? new HttpHeaders() : bearer(token);
        return restTemplate.exchange(baseUrl() + "/api/auth/register", HttpMethod.POST,
                new HttpEntity<>(request, headers), Map.class);
    }

    @Test
    void loginAndAccessProtectedEndpoint_succeedsForHrRole() {
        createUser("it-hr-user", "TestPass123!", "HR");
        String token = loginAndGetToken("it-hr-user");

        ResponseEntity<List> employeesResponse = restTemplate.exchange(
                baseUrl() + "/api/employees", HttpMethod.GET, new HttpEntity<>(bearer(token)), List.class);

        assertThat(employeesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void protectedEndpoint_rejectsRequestWithNoToken() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity(baseUrl() + "/api/employees", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void protectedEndpoint_rejectsWrongRole() {
        createUser("it-employee-user", "TestPass123!", "EMPLOYEE");
        String token = loginAndGetToken("it-employee-user");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/employees", HttpMethod.GET, new HttpEntity<>(bearer(token)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void register_withoutToken_isRejectedAndCreatesNothing() {
        ResponseEntity<Map> response = register(null, "it-anon-user", "ADMIN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(userRepository.findByUsername("it-anon-user")).isEmpty();
    }

    @Test
    void register_asEmployee_isRejected() {
        createUser("it-employee-register", "TestPass123!", "EMPLOYEE");
        String token = loginAndGetToken("it-employee-register");

        ResponseEntity<Map> response = register(token, "it-employee-made", "EMPLOYEE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(userRepository.findByUsername("it-employee-made")).isEmpty();
    }

    @Test
    void register_asHr_createsEmployeeButNotAdmin() {
        createUser("it-hr-register", "TestPass123!", "HR");
        String token = loginAndGetToken("it-hr-register");

        assertThat(register(token, "it-new-employee", "EMPLOYEE").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(register(token, "it-new-admin-by-hr", "ADMIN").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(userRepository.findByUsername("it-new-employee")).isPresent();
        assertThat(userRepository.findByUsername("it-new-admin-by-hr")).isEmpty();
    }

    @Test
    void register_asAdmin_canCreateAdmin() {
        createUser("it-admin-register", "TestPass123!", "ADMIN");
        String token = loginAndGetToken("it-admin-register");

        assertThat(register(token, "it-second-admin", "ADMIN").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userRepository.findByUsername("it-second-admin")).isPresent();
    }

    @Test
    void register_withInvalidRole_isRejected() {
        createUser("it-hr-invalid-role", "TestPass123!", "HR");
        String token = loginAndGetToken("it-hr-invalid-role");

        ResponseEntity<Map> response = register(token, "it-bad-role-user", "SUPERUSER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(userRepository.findByUsername("it-bad-role-user")).isEmpty();
    }
}
