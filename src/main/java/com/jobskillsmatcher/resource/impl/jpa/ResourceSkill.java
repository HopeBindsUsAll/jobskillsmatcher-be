package com.jobskillsmatcher.resource.impl.jpa;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resource_skill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSkill {

    @EmbeddedId
    private ResourceSkillId id;
}
