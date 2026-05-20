package com.jobskillsmatcher.module.impl.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ModuleSkillId implements Serializable {

    @Column(name = "module_id")
    private UUID moduleId;

    @Column(name = "skill_id", length = 255)
    private String skillId;
}
