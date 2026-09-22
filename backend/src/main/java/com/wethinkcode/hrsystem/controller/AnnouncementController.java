package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.AnnouncementRequest;
import com.wethinkcode.hrsystem.dto.AnnouncementResponse;
import com.wethinkcode.hrsystem.service.AnnouncementService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    // Login required - active announcements for everyone, plus the caller's branch (HR/Admin see all)
    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> getActive() {
        return ResponseEntity.ok(announcementService.getActiveForViewer().stream()
                .map(a -> AnnouncementResponse.from(a, announcementService.canCurrentUserDelete(a)))
                .toList());
    }

    // Login required - only announcements visible to the caller
    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> getById(@PathVariable Long id) {
        var announcement = announcementService.getByIdForViewer(id);
        return ResponseEntity.ok(AnnouncementResponse.from(announcement, announcementService.canCurrentUserDelete(announcement)));
    }

    // Login required - serves the poster image file (same visibility as the announcement)
    @GetMapping("/{id}/poster")
    public ResponseEntity<Resource> getPoster(@PathVariable Long id) {
        Resource poster = announcementService.getPoster(id);
        return ResponseEntity.ok().body(poster);
    }

    // PROTECTED - HR/Admin only, includes expired ones for management
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<AnnouncementResponse>> getAll() {
        return ResponseEntity.ok(announcementService.getAll().stream()
                .map(a -> AnnouncementResponse.from(a, announcementService.canCurrentUserDelete(a)))
                .toList());
    }

    // PROTECTED - HR/Admin post to any branches or everyone; Manager posts to their own branch only (enforced in service)
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<AnnouncementResponse> create(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam String category,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) List<Long> branchIds,
            @RequestParam(required = false) MultipartFile poster) {

        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setCategory(category);
        request.setBranchIds(branchIds);
        if (expiryDate != null && !expiryDate.isBlank()) {
            request.setExpiryDate(java.time.LocalDate.parse(expiryDate));
        }

        var created = announcementService.create(request, poster);
        return ResponseEntity.ok(AnnouncementResponse.from(created, announcementService.canCurrentUserDelete(created)));
    }

    // PROTECTED - HR/Admin delete anything; Manager deletes only announcements posted solely to their own branch (enforced in service)
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
