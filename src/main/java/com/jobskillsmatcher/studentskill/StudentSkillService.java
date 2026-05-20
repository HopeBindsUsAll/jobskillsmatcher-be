package com.jobskillsmatcher.studentskill;

import com.jobskillsmatcher.studentskill.port.rest.StudentSkillView;
import com.jobskillsmatcher.studentskill.port.rest.UpdateStudentSkillsRequest;

import java.util.List;
import java.util.UUID;

public interface StudentSkillService {

    List<StudentSkillView> listForStudent(UUID studentId);

    List<StudentSkillView> updateManualSkills(UUID studentId, UpdateStudentSkillsRequest req);

    List<StudentSkillView> applyModules(UUID studentId, List<UUID> moduleIds);

    void removeSkill(UUID studentId, String skillId);
}
