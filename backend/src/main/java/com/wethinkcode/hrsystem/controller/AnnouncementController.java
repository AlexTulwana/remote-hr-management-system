package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.AnnouncementRequest;
import com.wethinkcode.hrsystem.model.Announcement;
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

    // PUBLIC - no login required, shown on landing page carousel
    @GetMapping
    public ResponseEntity<List<Announcement>> getActive() {
        return ResponseEntity.ok(announcementService.getActive());
    }

    // PUBLIC - full detail view when someone clicks a slide
    @GetMapping("/{id}")
    public ResponseEntity<Announcement> getById(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getById(id));
    }

    // PUBLIC - serves the poster image file
    @GetMapping("/{id}/poster")
    public ResponseEntity<Resource> getPoster(@PathVariable Long id) {
        Resource poster = announcementService.getPoster(id);
        return ResponseEntity.ok().body(poster);
    }

    // PROTECTED - HR/Admin only, includes expired ones for management
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<Announcement>> getAll() {
        return ResponseEntity.ok(announcementService.getAll());
    }

    // PROTECTED - HR/Admin post anywhere (or all branches); Manager posts to their own branch only (enforced in service)
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Announcement> create(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam String category,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) MultipartFile poster) {

        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setCategory(category);
        request.setBranchId(branchId);
        if (expiryDate != null && !expiryDate.isBlank()) {
            request.setExpiryDate(java.time.LocalDate.parse(expiryDate));
        }

        return ResponseEntity.ok(announcementService.create(request, poster));
    }

    // PROTECTED - HR/Admin delete anything; Manager deletes their own branch's announcements only (enforced in service)
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}