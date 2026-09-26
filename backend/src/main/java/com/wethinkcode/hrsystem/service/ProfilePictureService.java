package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
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
import java.util.List;

@Service
public class ProfilePictureService {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png");

    private final String uploadDir;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public ProfilePictureService(EmployeeRepository employeeRepository,
                                  UserRepository userRepository,
                                  @Value("${profile-picture.upload-dir:uploads/profile-pictures/}") String uploadDir) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.uploadDir = uploadDir;
    }

    public static String contentTypeFor(String extension) {
        return switch (extension) {
            case ".png" -> "image/png";
            default -> "image/jpeg";
        };
    }

    public Employee upload(Long employeeId, MultipartFile file, String username) {
        User current = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (current.getEmployee() == null || !current.getEmployee().getId().equals(employeeId)) {
            throw new AccessDeniedException("You may only upload your own profile picture");
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File exceeds 2MB limit");
        }

        String originalFilename = Paths.get(file.getOriginalFilename()).getFileName().toString();
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Only JPG or PNG files are allowed");
        }

        try {
            Files.createDirectories(Paths.get(uploadDir));

            // remove any previous picture for this employee, regardless of its extension
            for (String ext : ALLOWED_EXTENSIONS) {
                Files.deleteIfExists(Paths.get(uploadDir).resolve(employeeId + ext));
            }

            String filename = employeeId + extension;
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            if (!filePath.startsWith(Paths.get(uploadDir).normalize())) {
                throw new RuntimeException("Invalid file path");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));
            employee.setProfilePicturePath(filePath.toString());
            return employeeRepository.save(employee);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Resource load(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));
        if (employee.getProfilePicturePath() == null) {
            throw new RuntimeException("No profile picture set for this employee");
        }
        try {
            return new UrlResource(Paths.get(employee.getProfilePicturePath()).toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not load profile picture", e);
        }
    }

    public String extensionFor(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));
        String path = employee.getProfilePicturePath();
        int dotIndex = path.lastIndexOf('.');
        return dotIndex > 0 ? path.substring(dotIndex).toLowerCase() : "";
    }
}
