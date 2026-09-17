package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.DocumentType;
import com.wethinkcode.hrsystem.model.EmployeeDocument;

import java.time.LocalDateTime;

public record EmployeeDocumentResponse(
        Long id,
        Long employeeId,
        DocumentType documentType,
        String fileName,
        String description,
        Long uploadedById,
        String uploadedByName,
        LocalDateTime uploadedAt
) {
    public static EmployeeDocumentResponse from(EmployeeDocument doc) {
        return new EmployeeDocumentResponse(
                doc.getId(),
                doc.getEmployee() != null ? doc.getEmployee().getId() : null,
                doc.getDocumentType(),
                doc.getFileName(),
                doc.getDescription(),
                doc.getUploadedBy() != null ? doc.getUploadedBy().getId() : null,
                doc.getUploadedBy() != null ? doc.getUploadedBy().getUsername() : null,
                doc.getUploadedAt()
        );
    }
}
