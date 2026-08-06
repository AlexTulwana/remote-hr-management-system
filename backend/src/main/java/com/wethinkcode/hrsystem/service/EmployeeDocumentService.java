package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.*;
import com.wethinkcode.hrsystem.repository.EmployeeDocumentRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EmployeeDocumentService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".jpg", ".jpeg", ".png", ".docx");
    private final String uploadDir = "uploads/employee-documents/";

    private final EmployeeDocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public EmployeeDocumentService(EmployeeDocumentRepository documentRepository,
                                   EmployeeRepository employeeRepository,
                                   UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    private User currentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean isHrOrAdmin(User user) {
        return "HR".equals(user.getRole()) || "ADMIN".equals(user.getRole());
    }

    public EmployeeDocument upload(Long employeeId, MultipartFile file, DocumentType type,
                                   String description, String uploaderUsername) {
        User uploader = currentUser(uploaderUsername);
        if (!isHrOrAdmin(uploader)) {
            throw new AccessDeniedException("Only HR or Admin may upload employee documents");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File exceeds 5MB limit");
        }

        String path = saveFile(file);

        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployee(employee);
        doc.setDocumentType(type);
        doc.setFileName(Paths.get(path).getFileName().toString());
        doc.setFilePath(path);
        doc.setDescription(description);
        doc.setUploadedBy(uploader);
        doc.setUploadedAt(LocalDateTime.now());

        return documentRepository.save(doc);
    }

    public List<EmployeeDocument> list(Long employeeId, String requesterUsername) {
        User requester = currentUser(requesterUsername);
        boolean isOwnRecord = requester.getEmployee() != null
                && requester.getEmployee().getId().equals(employeeId);

        if (!isHrOrAdmin(requester) && !isOwnRecord) {
            throw new AccessDeniedException("Not authorized to view these documents");
        }

        return documentRepository.findByEmployeeIdAndDeletedFalse(employeeId);
    }

    public EmployeeDocument get(Long documentId, String requesterUsername) {
        User requester = currentUser(requesterUsername);
        EmployeeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        boolean isOwnRecord = requester.getEmployee() != null
                && requester.getEmployee().getId().equals(doc.getEmployee().getId());

        if (doc.isDeleted() || (!isHrOrAdmin(requester) && !isOwnRecord)) {
            throw new AccessDeniedException("Not authorized to access this document");
        }

        return doc;
    }

    public void softDelete(Long documentId, String requesterUsername) {
        User requester = currentUser(requesterUsername);
        if (!isHrOrAdmin(requester)) {
            throw new AccessDeniedException("Only HR or Admin may delete employee documents");
        }

        EmployeeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        doc.setDeleted(true);
        doc.setDeletedAt(LocalDateTime.now());
        doc.setDeletedBy(requester);
        documentRepository.save(doc);
    }

    private String saveFile(MultipartFile file) {
        try {
            String originalFilename = Paths.get(file.getOriginalFilename()).getFileName().toString();
            String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

            String extension = "";
            int dotIndex = safeFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = safeFilename.substring(dotIndex).toLowerCase();
            }
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new RuntimeException("File type not allowed: " + extension);
            }

            Files.createDirectories(Paths.get(uploadDir));
            String filename = UUID.randomUUID() + extension;
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();

            if (!filePath.startsWith(Paths.get(uploadDir).normalize())) {
                throw new RuntimeException("Invalid file path");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}