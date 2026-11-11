package com.medvision.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private String status;
    private LocalDateTime timestamp = LocalDateTime.now();
}
