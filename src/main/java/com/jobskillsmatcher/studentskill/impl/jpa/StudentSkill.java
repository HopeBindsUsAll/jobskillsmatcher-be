package com.jobskillsmatcher.studentskill.impl.jpa;

import com.jobskillsmatcher.skill.model.ProficiencyLevel;
import com.jobskillsmatcher.studentskill.model.SkillSource;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "student_skill")
@Getter
@Setter
@NoArgsConstructor
public class StudentSkill {

    @EmbeddedId
    private StudentSkillId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProficiencyLevel proficiency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SkillSource source;

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
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
