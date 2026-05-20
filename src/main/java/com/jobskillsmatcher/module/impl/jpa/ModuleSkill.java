package com.jobskillsmatcher.module.impl.jpa;

import com.jobskillsmatcher.skill.model.ProficiencyLevel;
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
@Table(name = "module_skill")
@Getter
@Setter
@NoArgsConstructor
public class ModuleSkill {

    @EmbeddedId
    private ModuleSkillId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProficiencyLevel proficiency;
}
