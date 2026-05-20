package com.jobskillsmatcher.resource.impl.jpa;

import com.jobskillsmatcher.resource.model.ResourceDifficulty;
import com.jobskillsmatcher.resource.model.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "learning_resource")
@Getter
@Setter
@NoArgsConstructor
public class LearningResource {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ResourceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ResourceDifficulty difficulty;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 2000)
    private String url;

    @Column(nullable = false, length = 200)
    private String provider;

    @Column(name = "url_alive", nullable = false)
    private boolean urlAlive = true;

    @Column(name = "last_validated_at")
    private OffsetDateTime lastValidatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (description == null) description = "";
        if (provider == null) provider = "";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
