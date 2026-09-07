package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Payslip;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.PayslipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PayslipController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class PayslipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PayslipService payslipService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "july-payslip.pdf", "application/pdf",
                "dummy content".getBytes(StandardCharsets.UTF_8));
    }

    // ---- upload() ----

    @Test
    void upload_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(multipart("/api/payslips/1")
                        .file(file())
                        .param("payPeriod", "2026-07"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void upload_hrRole_isAllowed() throws Exception {
        Payslip payslip = new Payslip();
        payslip.setEmployee(new Employee());
        when(payslipService.upload(anyLong(), anyString(), any())).thenReturn(payslip);

        mockMvc.perform(multipart("/api/payslips/1")
                        .file(file())
                        .param("payPeriod", "2026-07"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void upload_adminRole_isAllowed() throws Exception {
        Payslip payslip = new Payslip();
        payslip.setEmployee(new Employee());
        when(payslipService.upload(anyLong(), anyString(), any())).thenReturn(payslip);

        mockMvc.perform(multipart("/api/payslips/1")
                        .file(file())
                        .param("payPeriod", "2026-07"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void upload_managerRole_isForbidden() throws Exception {
        mockMvc.perform(multipart("/api/payslips/1")
                        .file(file())
                        .param("payPeriod", "2026-07"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void upload_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(multipart("/api/payslips/1")
                        .file(file())
                        .param("payPeriod", "2026-07"))
                .andExpect(status().isForbidden());
    }

    // ---- getByEmployee() ----

    @Test
    void getByEmployee_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/payslips/employee/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getByEmployee_hrRole_isAllowed() throws Exception {
        when(payslipService.getByEmployee(1L)).thenReturn(List.of(new Payslip()));

        mockMvc.perform(get("/api/payslips/employee/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByEmployee_employeeViewingOther_deniedByService() throws Exception {
        when(payslipService.getByEmployee(anyLong()))
                .thenThrow(new AccessDeniedException("Not authorized to view these payslips"));

        mockMvc.perform(get("/api/payslips/employee/1"))
                .andExpect(status().isForbidden());
    }

    // ---- download() ----

    @Test
    void download_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/payslips/7/download"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void download_hrRole_isAllowed() throws Exception {
        Resource resource = new ByteArrayResource("payslip bytes".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "july-payslip.pdf";
            }
        };
        when(payslipService.downloadFile(7L)).thenReturn(resource);

        mockMvc.perform(get("/api/payslips/7/download"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void download_employeeViewingOther_deniedByService() throws Exception {
        when(payslipService.downloadFile(anyLong()))
                .thenThrow(new AccessDeniedException("Not authorized to download this payslip"));

        mockMvc.perform(get("/api/payslips/9/download"))
                .andExpect(status().isForbidden());
    }
}
