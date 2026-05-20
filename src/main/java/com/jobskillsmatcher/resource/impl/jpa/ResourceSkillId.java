package com.jobskillsmatcher.resource.impl.jpa;

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
public class ResourceSkillId implements Serializable {

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "skill_id", nullable = false, length = 255)
    private String skillId;
}
