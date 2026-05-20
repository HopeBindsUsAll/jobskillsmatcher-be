package com.jobskillsmatcher.studentskill.impl.jpa;

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
public class StudentSkillId implements Serializable {

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "skill_id", length = 255)
    private String skillId;
}
