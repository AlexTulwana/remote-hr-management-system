package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.ApplicationDocument;
import com.wethinkcode.hrsystem.model.DocumentType;

import java.time.LocalDateTime;

// No raw filePath - downloads go through a dedicated endpoint instead.
public record ApplicationDocumentResponse(
        Long id,
        DocumentType documentType,
        String fileName,
        LocalDateTime uploadedAt
) {
    public static ApplicationDocumentResponse from(ApplicationDocument d) {
        return new ApplicationDocumentResponse(
                d.getId(),
                d.getDocumentType(),
                d.getFileName(),
                d.getUploadedAt()
        );
    }
}
