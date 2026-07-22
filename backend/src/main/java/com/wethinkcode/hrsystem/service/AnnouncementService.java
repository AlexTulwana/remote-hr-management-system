package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.AnnouncementRequest;
import com.wethinkcode.hrsystem.model.Announcement;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.AnnouncementRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final String uploadDir = "uploads/announcements/";

    public AnnouncementService(AnnouncementRepository announcementRepository, UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
    }

    public Announcement create(AnnouncementRequest request, MultipartFile poster) {
        User postedBy = userRepository.findById(request.getPostedById())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setCategory(request.getCategory());
        announcement.setExpiryDate(request.getExpiryDate());
        announcement.setPostedDate(LocalDate.now());
        announcement.setPostedBy(postedBy);

        if (poster != null && !poster.isEmpty()) {
            announcement.setPosterImagePath(saveFile(poster));
        }

        return announcementRepository.save(announcement);
    }

    public List<Announcement> getActive() {
        return announcementRepository.findActive(LocalDate.now());
    }

    public List<Announcement> getAll() {
        return announcementRepository.findAll();
    }

    public Announcement getById(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
    }

    public void delete(Long id) {
        announcementRepository.deleteById(id);
    }

    public Resource getPoster(Long id) {
        Announcement announcement = getById(id);
        if (announcement.getPosterImagePath() == null) {
            throw new RuntimeException("No poster image for this announcement");
        }
        try {
            Path filePath = Paths.get(announcement.getPosterImagePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("Poster file not found on disk");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error loading poster: " + e.getMessage());
        }
    }

    private String saveFile(MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store poster image", e);
        }
    }
}