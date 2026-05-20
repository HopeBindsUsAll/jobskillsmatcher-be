package com.jobskillsmatcher.job.impl.jpa;

import com.jobskillsmatcher.job.model.Requirement;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "job_skill")
@Getter
@Setter
@NoArgsConstructor
public class JobSkill {

    @EmbeddedId
    private JobSkillId id;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private Requirement requirement;
}
