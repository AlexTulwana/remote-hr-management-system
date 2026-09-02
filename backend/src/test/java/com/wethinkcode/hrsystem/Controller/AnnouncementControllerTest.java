package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Announcement;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.AnnouncementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnnouncementController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnnouncementService announcementService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---- PUBLIC endpoints: no auth required ----

    @Test
    void getActive_noAuth_isPublic() throws Exception {
        when(announcementService.getActive()).thenReturn(List.of(new Announcement()));

        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_noAuth_isPublic() throws Exception {
        Announcement announcement = new Announcement();
        announcement.setId(1L);
        when(announcementService.getById(1L)).thenReturn(announcement);

        mockMvc.perform(get("/api/announcements/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getPoster_noAuth_isPublic() throws Exception {
        Resource poster = new ByteArrayResource("fake-image-bytes".getBytes());
        when(announcementService.getPoster(1L)).thenReturn(poster);

        mockMvc.perform(get("/api/announcements/1/poster"))
                .andExpect(status().isOk());
    }

    // ---- PROTECTED: getAll (HR/ADMIN only) ----

    @Test
    void getAll_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/announcements/all"))
                .andExpect(status().isForbidden());   // was isUnauthorized()
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAll_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/announcements/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAll_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/announcements/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getAll_hrRole_isAllowed() throws Exception {
        when(announcementService.getAll()).thenReturn(List.of(new Announcement()));

        mockMvc.perform(get("/api/announcements/all"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_adminRole_isAllowed() throws Exception {
        when(announcementService.getAll()).thenReturn(List.of(new Announcement()));

        mockMvc.perform(get("/api/announcements/all"))
                .andExpect(status().isOk());
    }

    // ---- PROTECTED: create (HR/ADMIN/MANAGER) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void create_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(multipart("/api/announcements")
                        .param("title", "Test")
                        .param("content", "Body")
                        .param("category", "General")
                        .with(req -> { req.setMethod("POST"); return req; }))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_managerRole_isAllowed() throws Exception {
        Announcement created = new Announcement();
        created.setId(5L);
        when(announcementService.create(any(), any())).thenReturn(created);

        mockMvc.perform(multipart("/api/announcements")
                        .param("title", "Branch meeting")
                        .param("content", "Body")
                        .param("category", "Staff Meeting")
                        .param("branchId", "1")
                        .with(req -> { req.setMethod("POST"); return req; }))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void create_withPosterFile_hrRole_isAllowed() throws Exception {
        Announcement created = new Announcement();
        created.setId(6L);
        when(announcementService.create(any(), any())).thenReturn(created);

        MockMultipartFile poster = new MockMultipartFile(
                "poster", "poster.png", "image/png", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/api/announcements")
                        .file(poster)
                        .param("title", "Company-wide notice")
                        .param("content", "Body")
                        .param("category", "Notice")
                        .with(req -> { req.setMethod("POST"); return req; }))
                .andExpect(status().isOk());
    }

    // ---- PROTECTED: delete (HR/ADMIN/MANAGER) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void delete_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/announcements/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void delete_managerRole_isAllowed() throws Exception {
        mockMvc.perform(delete("/api/announcements/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "HR")
    void delete_hrRole_isAllowed() throws Exception {
        mockMvc.perform(delete("/api/announcements/1"))
                .andExpect(status().isNoContent());
    }
}