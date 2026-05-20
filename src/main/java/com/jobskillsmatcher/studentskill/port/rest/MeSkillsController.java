package com.jobskillsmatcher.studentskill.port.rest;

import com.jobskillsmatcher.context.security.CurrentUser;
import com.jobskillsmatcher.studentskill.StudentSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeSkillsController {

    private final StudentSkillService studentSkillService;

    @GetMapping("/skills")
    public List<StudentSkillView> listMySkills() {
        return studentSkillService.listForStudent(CurrentUser.requireId());
    }

    @PutMapping("/skills")
    public List<StudentSkillView> updateMySkills(@Valid @RequestBody UpdateStudentSkillsRequest req) {
        return studentSkillService.updateManualSkills(CurrentUser.requireId(), req);
    }

    @DeleteMapping("/skills")
    public ResponseEntity<Void> removeMySkill(@RequestParam("skillId") String skillId) {
        studentSkillService.removeSkill(CurrentUser.requireId(), skillId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/modules")
    public List<StudentSkillView> applyModules(@Valid @RequestBody ApplyModulesRequest req) {
        return studentSkillService.applyModules(CurrentUser.requireId(), req.moduleIds());
    }
}
