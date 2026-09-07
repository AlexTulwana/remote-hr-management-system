package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.DocumentType;
import com.wethinkcode.hrsystem.model.EmployeeDocument;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.EmployeeDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeDocumentController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class EmployeeDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeDocumentService documentService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @TempDir
    Path tempDir;

    // ---- upload (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void upload_employeeRole_isForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/employees/1/documents")
                        .file(file)
                        .param("documentType", "CV")
                        .with(req -> { req.setMethod("POST"); return req; }))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hrtest2", roles = "HR")
    void upload_hrRole_isAllowed() throws Exception {
        EmployeeDocument saved = new EmployeeDocument();
        saved.setFileName("abc.pdf");
        when(documentService.upload(anyLong(), any(), any(DocumentType.class), any(), anyString()))
                .thenReturn(saved);

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/employees/1/documents")
                        .file(file)
                        .param("documentType", "CV")
                        .with(req -> { req.setMethod("POST"); return req; }))
                .andExpect(status().isCreated());
    }

    // ---- list: no @PreAuthorize, access checked in service ----

    @Test
    @WithMockUser(username = "emptest1", roles = "EMPLOYEE")
    void list_allowedByService_returnsOk() throws Exception {
        when(documentService.list(anyLong(), anyString())).thenReturn(List.of(new EmployeeDocument()));

        mockMvc.perform(get("/api/employees/1/documents"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "other", roles = "EMPLOYEE")
    void list_deniedByService_returnsForbidden() throws Exception {
        when(documentService.list(anyLong(), anyString()))
                .thenThrow(new AccessDeniedException("Not authorized to view these documents"));

        mockMvc.perform(get("/api/employees/1/documents"))
                .andExpect(status().isForbidden());
    }

    // ---- download: no @PreAuthorize, access checked in service ----

    @Test
    @WithMockUser(username = "emptest1", roles = "EMPLOYEE")
    void download_allowedByService_returnsOk() throws Exception {
        Path realFile = tempDir.resolve("cv.pdf");
        Files.writeString(realFile, "fake pdf content");

        EmployeeDocument doc = new EmployeeDocument();
        doc.setFilePath(realFile.toString());
        doc.setFileName("cv.pdf");
        when(documentService.get(anyLong(), anyString())).thenReturn(doc);

        mockMvc.perform(get("/api/employees/documents/5/download"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "other", roles = "EMPLOYEE")
    void download_deniedByService_returnsForbidden() throws Exception {
        when(documentService.get(anyLong(), anyString()))
                .thenThrow(new AccessDeniedException("Not authorized to access this document"));

        mockMvc.perform(get("/api/employees/documents/5/download"))
                .andExpect(status().isForbidden());
    }

    // ---- delete (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void delete_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/employees/documents/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hrtest2", roles = "HR")
    void delete_hrRole_isAllowed() throws Exception {
        mockMvc.perform(delete("/api/employees/documents/5"))
                .andExpect(status().isNoContent());
    }
}