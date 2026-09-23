package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.AnnouncementRequest;
import com.wethinkcode.hrsystem.model.Announcement;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.AnnouncementRepository;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final CurrentUserService currentUserService;
    private final String uploadDir;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               UserRepository userRepository,
                               BranchRepository branchRepository,
                               CurrentUserService currentUserService,
                               @Value("${announcement.upload-dir:uploads/announcements/}") String uploadDir) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.currentUserService = currentUserService;
        this.uploadDir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
    }

    public Announcement create(AnnouncementRequest request, MultipartFile poster) {
        if (request.getExpiryDate() == null) {
            throw new RuntimeException("Expiry date is required");
        }
        User postedBy = currentUserService.getCurrentUser();
        String role = postedBy.getRole();
        List<Long> requestedIds = request.getBranchIds() == null
                ? List.of()
                : request.getBranchIds().stream().distinct().toList();

        Set<Branch> targetBranches = new HashSet<>();

        if (role.equals("MANAGER")) {
            Employee employee = postedBy.getEmployee();
            if (employee == null || employee.getBranch() == null) {
                throw new AccessDeniedException("Manager is not assigned to a branch");
            }
            if (requestedIds.size() != 1 || !requestedIds.get(0).equals(employee.getBranch().getId())) {
                throw new AccessDeniedException("Managers may only post announcements to their own branch");
            }
            targetBranches.add(employee.getBranch());
        } else if (role.equals("HR") || role.equals("ADMIN")) {
            if (!requestedIds.isEmpty()) {
                List<Branch> found = branchRepository.findAllById(requestedIds);
                if (found.size() != requestedIds.size()) {
                    throw new RuntimeException("Branch not found");
                }
                targetBranches.addAll(found);
            }
        } else {
            throw new AccessDeniedException("You are not authorized to post announcements");
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setCategory(request.getCategory());
        announcement.setExpiryDate(request.getExpiryDate());
        announcement.setPostedDate(LocalDate.now());
        announcement.setPostedBy(postedBy);
        announcement.setBranches(targetBranches);

        if (poster != null && !poster.isEmpty()) {
            announcement.setPosterImagePath(saveFile(poster));
        }

        return announcementRepository.save(announcement);
    }

    private boolean isVisibleTo(Announcement announcement, boolean hrOrAdmin, Long branchId) {
        if (hrOrAdmin) {
            return true;
        }
        if (announcement.getBranches() == null || announcement.getBranches().isEmpty()) {
            return true;
        }
        return branchId != null
                && announcement.getBranches().stream().anyMatch(b -> branchId.equals(b.getId()));
    }

    public List<Announcement> getActiveForViewer() {
        boolean hrOrAdmin = currentUserService.isHrOrAdmin();
        Long branchId = hrOrAdmin ? null : currentUserService.getCurrentBranchId();
        return announcementRepository.findActive(LocalDate.now()).stream()
                .filter(a -> isVisibleTo(a, hrOrAdmin, branchId))
                .toList();
    }

    public List<Announcement> getAll() {
        return announcementRepository.findAll();
    }

    public Announcement getById(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
    }

    public Announcement getByIdForViewer(Long id) {
        Announcement announcement = getById(id);
        boolean hrOrAdmin = currentUserService.isHrOrAdmin();
        Long branchId = hrOrAdmin ? null : currentUserService.getCurrentBranchId();
        if (!isVisibleTo(announcement, hrOrAdmin, branchId)) {
            // Deliberately indistinguishable from a missing announcement.
            throw new RuntimeException("Announcement not found");
        }
        return announcement;
    }

    private boolean isDeletableBy(Announcement announcement, User currentUser) {
        String role = currentUser.getRole();
        if (role.equals("HR") || role.equals("ADMIN")) {
            return true;
        }
        if (role.equals("MANAGER")) {
            Employee employee = currentUser.getEmployee();
            Branch managerBranch = (employee != null) ? employee.getBranch() : null;
            Set<Branch> targets = announcement.getBranches();
            boolean ownBranchOnly = managerBranch != null
                    && targets != null
                    && targets.size() == 1
                    && targets.iterator().next().getId().equals(managerBranch.getId());
            boolean isAuthor = announcement.getPostedBy() != null
                    && announcement.getPostedBy().getId() != null
                    && announcement.getPostedBy().getId().equals(currentUser.getId());
            return ownBranchOnly && isAuthor;
        }
        return false;
    }

    public boolean canCurrentUserDelete(Announcement announcement) {
        return isDeletableBy(announcement, currentUserService.getCurrentUser());
    }

    public void delete(Long id) {
        Announcement announcement = getById(id);
        User currentUser = currentUserService.getCurrentUser();

        if (!isDeletableBy(announcement, currentUser)) {
            String role = currentUser.getRole();
            if (role.equals("MANAGER")) {
                throw new AccessDeniedException("Managers may only delete their own announcements posted solely to their own branch");
            }
            throw new AccessDeniedException("You are not authorized to delete announcements");
        }

        announcementRepository.deleteById(id);
    }

    public Resource getPoster(Long id) {
        Announcement announcement = getByIdForViewer(id);
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
