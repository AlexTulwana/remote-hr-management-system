package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.CurrentUserResponse;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import com.wethinkcode.hrsystem.service.ProfilePictureService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ProfilePictureController {

    private final ProfilePictureService profilePictureService;
    private final CurrentUserService currentUserService;

    public ProfilePictureController(ProfilePictureService profilePictureService,
                                     CurrentUserService currentUserService) {
        this.profilePictureService = profilePictureService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/users/me/profile-picture")
    public ResponseEntity<CurrentUserResponse> upload(@RequestParam("file") MultipartFile file) {
        User current = currentUserService.getCurrentUser();
        if (current.getEmployee() == null) {
            throw new RuntimeException("No employee record is linked to this account");
        }
        profilePictureService.upload(current.getEmployee().getId(), file, current.getUsername());
        return ResponseEntity.ok(CurrentUserResponse.from(currentUserService.getCurrentUser()));
    }

    @GetMapping("/employees/{id}/profile-picture")
    public ResponseEntity<Resource> get(@PathVariable Long id) {
        Resource resource = profilePictureService.load(id);
        String extension = profilePictureService.extensionFor(id);
        MediaType mediaType = MediaType.parseMediaType(ProfilePictureService.contentTypeFor(extension));
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }
}
