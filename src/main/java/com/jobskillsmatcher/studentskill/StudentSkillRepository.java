package com.jobskillsmatcher.studentskill;

import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkillId;

import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StudentSkillRepository extends JpaRepository<StudentSkill, StudentSkillId> {

    List<StudentSkill> findAllByIdStudentId(UUID studentId);

    @Modifying
    @Query("delete from StudentSkill s where s.id.studentId = :studentId and s.id.skillId = :skillId")
    int deleteByStudentIdAndSkillId(@Param("studentId") UUID studentId, @Param("skillId") String skillId);
}
