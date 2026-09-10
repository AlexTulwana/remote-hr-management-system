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
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerLoginAndAccessProtectedEndpoint_succeedsForHrRole() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("it-hr-user");
        register.setPassword("TestPass123!");
        register.setRole("HR");

        ResponseEntity<Map> registerResponse =
                restTemplate.postForEntity(baseUrl() + "/api/auth/register", register, Map.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        LoginRequest login = new LoginRequest();
        login.setUsername("it-hr-user");
        login.setPassword("TestPass123!");

        ResponseEntity<Map> loginResponse =
                restTemplate.postForEntity(baseUrl() + "/api/auth/login", login, Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) loginResponse.getBody().get("token");
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        ResponseEntity<List> employeesResponse = restTemplate.exchange(
                baseUrl() + "/api/employees", HttpMethod.GET, new HttpEntity<>(headers), List.class);

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
        RegisterRequest register = new RegisterRequest();
        register.setUsername("it-employee-user");
        register.setPassword("TestPass123!");
        register.setRole("EMPLOYEE");
        restTemplate.postForEntity(baseUrl() + "/api/auth/register", register, Map.class);

        LoginRequest login = new LoginRequest();
        login.setUsername("it-employee-user");
        login.setPassword("TestPass123!");
        ResponseEntity<Map> loginResponse =
                restTemplate.postForEntity(baseUrl() + "/api/auth/login", login, Map.class);
        String token = (String) loginResponse.getBody().get("token");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/employees", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
