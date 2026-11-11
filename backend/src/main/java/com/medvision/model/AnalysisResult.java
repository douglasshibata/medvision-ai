package com.medvision.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class AnalysisResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "studyUID is required")
    @Size(max = 128, message = "studyUID must not exceed 128 characters")
    private String studyUID;

    @NotBlank(message = "instanceUID is required")
    @Size(max = 128, message = "instanceUID must not exceed 128 characters")
    private String instanceUID;

    @NotBlank(message = "patientName is required")
    @Size(max = 256, message = "patientName must not exceed 256 characters")
    private String patientName;

    @NotNull(message = "confidence is required")
    private Double confidence;

    private boolean anomalyDetected;

    @Column(columnDefinition = "TEXT")
    private String findings;

    @Column(columnDefinition = "TEXT")
    private String measurements; // Store as JSON string

    @Column(columnDefinition = "TEXT")
    private String structuredReport; // Store as JSON string

    private String classification;
    @Column(columnDefinition = "TEXT")
    private String classificationScores;

    @NotBlank(message = "status is required")
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED

    private Integer priorityLevel; // 1: LOW, 2: MEDIUM, 3: CRITICAL
    private String priority; // CRITICAL, MEDIUM, LOW

    private LocalDateTime createdAt = LocalDateTime.now();
}
