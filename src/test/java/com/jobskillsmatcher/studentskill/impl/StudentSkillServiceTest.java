package com.jobskillsmatcher.studentskill.impl;

import com.jobskillsmatcher.studentskill.StudentSkillService;

import com.jobskillsmatcher.module.impl.jpa.ModuleSkill;
import com.jobskillsmatcher.module.impl.jpa.ModuleSkillId;
import com.jobskillsmatcher.module.ModuleSkillRepository;
import com.jobskillsmatcher.skill.SkillTestDataFactory;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import com.jobskillsmatcher.skill.model.ProficiencyLevel;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkill;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkillId;
import com.jobskillsmatcher.studentskill.StudentSkillRepository;
import com.jobskillsmatcher.studentskill.model.SkillSource;
import com.jobskillsmatcher.studentskill.port.rest.StudentSkillView;
import com.jobskillsmatcher.studentskill.port.rest.UpdateStudentSkillsRequest;
import com.jobskillsmatcher.user.UserTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentSkillServiceTest {

    private static final UUID STUDENT_ID = UserTestDataFactory.STUDENT_ID;
    private static final UUID MODULE_SE_ID = UUID.fromString("11111111-1111-1111-1111-000000000001");
    private static final UUID MODULE_DB_ID = UUID.fromString("11111111-1111-1111-1111-000000000003");

    @Mock
    StudentSkillRepository studentSkillRepository;

    @Mock
    ModuleSkillRepository moduleSkillRepository;

    @Mock
    SkillRepository skillRepository;

    @InjectMocks
    StudentSkillServiceImpl service;

    Map<StudentSkillId, StudentSkill> store;

    @BeforeEach
    void setUp() {
        store = new HashMap<>();
        lenient().when(studentSkillRepository.findAllByIdStudentId(STUDENT_ID))
                .thenAnswer(inv -> new ArrayList<>(store.values()));
        lenient().when(studentSkillRepository.save(any(StudentSkill.class))).thenAnswer(inv -> {
            StudentSkill e = inv.getArgument(0);
            store.put(e.getId(), e);
            return e;
        });
    }

    @Test
    void successApplyModulesSeedsModuleSourcedSkills() {
        stubSkillRepo(SkillTestDataFactory.java(), SkillTestDataFactory.sql());
        when(moduleSkillRepository.findAllByIdModuleIdIn(List.of(MODULE_SE_ID, MODULE_DB_ID))).thenReturn(List.of(
                moduleSkill(MODULE_SE_ID, SkillTestDataFactory.JAVA_ID, ProficiencyLevel.INTERMEDIATE),
                moduleSkill(MODULE_DB_ID, SkillTestDataFactory.SQL_ID, ProficiencyLevel.INTERMEDIATE)
        ));

        List<StudentSkillView> result = service.applyModules(STUDENT_ID, List.of(MODULE_SE_ID, MODULE_DB_ID));

        assertThat(result).extracting(StudentSkillView::skillId)
                .containsExactlyInAnyOrder(SkillTestDataFactory.JAVA_ID, SkillTestDataFactory.SQL_ID);
        assertThat(result).allMatch(v -> v.source() == SkillSource.MODULE);
    }

    @Test
    void successApplyModulesUpgradesProficiencyToMax() {
        stubSkillRepo(SkillTestDataFactory.java());
        seedExisting(SkillTestDataFactory.JAVA_ID, ProficiencyLevel.BEGINNER, SkillSource.MODULE);
        when(moduleSkillRepository.findAllByIdModuleIdIn(List.of(MODULE_SE_ID))).thenReturn(List.of(
                moduleSkill(MODULE_SE_ID, SkillTestDataFactory.JAVA_ID, ProficiencyLevel.ADVANCED)
        ));

        List<StudentSkillView> result = service.applyModules(STUDENT_ID, List.of(MODULE_SE_ID));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).proficiency()).isEqualTo(ProficiencyLevel.ADVANCED);
        assertThat(result.get(0).source()).isEqualTo(SkillSource.MODULE);
    }

    @Test
    void successApplyModulesPreservesManualEditsOnConflict() {
        stubSkillRepo(SkillTestDataFactory.java());
        seedExisting(SkillTestDataFactory.JAVA_ID, ProficiencyLevel.EXPERT, SkillSource.MANUAL);
        when(moduleSkillRepository.findAllByIdModuleIdIn(List.of(MODULE_SE_ID))).thenReturn(List.of(
                moduleSkill(MODULE_SE_ID, SkillTestDataFactory.JAVA_ID, ProficiencyLevel.INTERMEDIATE)
        ));

        List<StudentSkillView> result = service.applyModules(STUDENT_ID, List.of(MODULE_SE_ID));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).proficiency()).isEqualTo(ProficiencyLevel.EXPERT);
        assertThat(result.get(0).source()).isEqualTo(SkillSource.MANUAL);
    }

    @Test
    void successUpdateManualSkillsInsertsAndPromotesToManual() {
        stubSkillRepo(SkillTestDataFactory.java(), SkillTestDataFactory.sql());
        seedExisting(SkillTestDataFactory.JAVA_ID, ProficiencyLevel.BEGINNER, SkillSource.MODULE);

        UpdateStudentSkillsRequest req = new UpdateStudentSkillsRequest(List.of(
                new UpdateStudentSkillsRequest.Item(SkillTestDataFactory.JAVA_ID, ProficiencyLevel.EXPERT),
                new UpdateStudentSkillsRequest.Item(SkillTestDataFactory.SQL_ID, ProficiencyLevel.INTERMEDIATE)
        ));

        List<StudentSkillView> result = service.updateManualSkills(STUDENT_ID, req);

        assertThat(result).hasSize(2);
        assertThat(store.get(new StudentSkillId(STUDENT_ID, SkillTestDataFactory.JAVA_ID)).getSource())
                .isEqualTo(SkillSource.MANUAL);
        assertThat(store.get(new StudentSkillId(STUDENT_ID, SkillTestDataFactory.JAVA_ID)).getProficiency())
                .isEqualTo(ProficiencyLevel.EXPERT);
        assertThat(store.get(new StudentSkillId(STUDENT_ID, SkillTestDataFactory.SQL_ID)).getSource())
                .isEqualTo(SkillSource.MANUAL);
    }

    @Test
    void successUpdateManualSkillsDeletesRemovedManualButKeepsModule() {
        stubSkillRepo(SkillTestDataFactory.sql());
        seedExisting(SkillTestDataFactory.JAVA_ID, ProficiencyLevel.BEGINNER, SkillSource.MODULE);
        seedExisting(SkillTestDataFactory.PYTHON_ID, ProficiencyLevel.INTERMEDIATE, SkillSource.MANUAL);

        UpdateStudentSkillsRequest req = new UpdateStudentSkillsRequest(List.of(
                new UpdateStudentSkillsRequest.Item(SkillTestDataFactory.SQL_ID, ProficiencyLevel.BEGINNER)
        ));

        service.updateManualSkills(STUDENT_ID, req);

        ArgumentCaptor<Iterable<StudentSkill>> captor = ArgumentCaptor.captor();
        verify(studentSkillRepository, atLeastOnce()).deleteAll(captor.capture());
        List<StudentSkill> deleted = new ArrayList<>();
        for (StudentSkill e : captor.getValue()) {
            deleted.add(e);
        }
        assertThat(deleted).extracting(e -> e.getId().getSkillId()).containsExactly(SkillTestDataFactory.PYTHON_ID);
    }

    @Test
    void failedUpdateManualSkillsWithUnknownSkillId() {
        when(skillRepository.findAllById(anyList())).thenReturn(List.of());

        UpdateStudentSkillsRequest req = new UpdateStudentSkillsRequest(List.of(
                new UpdateStudentSkillsRequest.Item("esco/skill/unknown", ProficiencyLevel.BEGINNER)
        ));

        assertThatThrownBy(() -> service.updateManualSkills(STUDENT_ID, req))
                .isInstanceOf(StudentSkillServiceImpl.UnknownSkillException.class);
    }

    @Test
    void successRemoveSkill() {
        when(studentSkillRepository.deleteByStudentIdAndSkillId(STUDENT_ID, SkillTestDataFactory.JAVA_ID))
                .thenReturn(1);

        service.removeSkill(STUDENT_ID, SkillTestDataFactory.JAVA_ID);

        verify(studentSkillRepository).deleteByStudentIdAndSkillId(STUDENT_ID, SkillTestDataFactory.JAVA_ID);
    }

    @Test
    void failedRemoveSkillWhenMissing() {
        when(studentSkillRepository.deleteByStudentIdAndSkillId(STUDENT_ID, "esco/skill/unknown"))
                .thenReturn(0);

        assertThatThrownBy(() -> service.removeSkill(STUDENT_ID, "esco/skill/unknown"))
                .isInstanceOf(StudentSkillServiceImpl.StudentSkillNotFoundException.class);
    }

    private void seedExisting(String skillId, ProficiencyLevel level, SkillSource source) {
        StudentSkill row = new StudentSkill();
        row.setId(new StudentSkillId(STUDENT_ID, skillId));
        row.setProficiency(level);
        row.setSource(source);
        store.put(row.getId(), row);
    }

    private void stubSkillRepo(Skill... skills) {
        when(skillRepository.findAllById(anyList())).thenReturn(List.of(skills));
    }

    private ModuleSkill moduleSkill(UUID moduleId, String skillId, ProficiencyLevel level) {
        ModuleSkill e = new ModuleSkill();
        e.setId(new ModuleSkillId(moduleId, skillId));
        e.setProficiency(level);
        return e;
    }
}
