package com.jobskillsmatcher.matching.impl;

import com.jobskillsmatcher.matching.ReadinessService;

import com.jobskillsmatcher.context.cache.CacheConfig;
import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.JobRepository;
import com.jobskillsmatcher.job.impl.jpa.JobSkill;
import com.jobskillsmatcher.job.JobSkillRepository;
import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.matching.MatchingProperties;
import com.jobskillsmatcher.matching.model.MatchedSkill;
import com.jobskillsmatcher.matching.model.MissingSkill;
import com.jobskillsmatcher.matching.model.ScoreBreakdown;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import com.jobskillsmatcher.skill.model.ProficiencyLevel;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkill;
import com.jobskillsmatcher.studentskill.StudentSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReadinessServiceImpl implements ReadinessService {

    private final StudentSkillRepository studentSkillRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final SkillRepository skillRepository;
    private final MatchingProperties props;

    @Cacheable(value = CacheConfig.READINESS_SCORE, key = "#studentId + ':' + #jobId")
    @Transactional(readOnly = true)
    public ScoreBreakdown score(UUID studentId, UUID jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return empty();
        }
        List<StudentSkill> ownedRows = studentSkillRepository.findAllByIdStudentId(studentId);
        List<JobSkill> jobSkills = jobSkillRepository.findAllByIdJobId(jobId);
        return scoreInternal(ownedRows, jobSkills);
    }

    // Batch variant: scores N jobs for one student, reusing the loaded skill catalog.
    @Transactional(readOnly = true)
    public Map<UUID, ScoreBreakdown> scoreMany(UUID studentId, List<UUID> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Map.of();
        }
        List<StudentSkill> ownedRows = studentSkillRepository.findAllByIdStudentId(studentId);
        List<JobSkill> allLinks = jobSkillRepository.findAllByIdJobIdIn(jobIds);
        Map<UUID, List<JobSkill>> byJob = new HashMap<>();
        for (JobSkill l : allLinks) {
            byJob.computeIfAbsent(l.getId().getJobId(), k -> new ArrayList<>()).add(l);
        }
        // Load every referenced skill once.
        Set<String> skillIds = new HashSet<>();
        for (StudentSkill s : ownedRows) skillIds.add(s.getId().getSkillId());
        for (JobSkill l : allLinks) skillIds.add(l.getId().getSkillId());
        Map<String, Skill> catalog = loadSkillCatalog(skillIds);

        Map<UUID, ScoreBreakdown> out = new LinkedHashMap<>(jobIds.size());
        for (UUID jobId : jobIds) {
            List<JobSkill> links = byJob.getOrDefault(jobId, List.of());
            out.put(jobId, scoreFromLoaded(ownedRows, links, catalog));
        }
        return out;
    }

    private ScoreBreakdown scoreInternal(List<StudentSkill> ownedRows, List<JobSkill> jobSkills) {
        Set<String> skillIds = new HashSet<>();
        for (StudentSkill s : ownedRows) skillIds.add(s.getId().getSkillId());
        for (JobSkill l : jobSkills) skillIds.add(l.getId().getSkillId());
        Map<String, Skill> catalog = loadSkillCatalog(skillIds);
        return scoreFromLoaded(ownedRows, jobSkills, catalog);
    }

    private ScoreBreakdown scoreFromLoaded(List<StudentSkill> ownedRows,
                                           List<JobSkill> jobSkills,
                                           Map<String, Skill> catalog) {
        Map<String, ProficiencyLevel> ownedProf = new HashMap<>();
        Map<String, Double> studentW = new HashMap<>();
        for (StudentSkill s : ownedRows) {
            String id = s.getId().getSkillId();
            ownedProf.put(id, s.getProficiency());
            studentW.put(id, props.weightFor(s.getProficiency()));
        }
        Map<String, Double> jobW = new HashMap<>();
        Map<String, Requirement> jobReq = new HashMap<>();
        for (JobSkill l : jobSkills) {
            String id = l.getId().getSkillId();
            jobReq.put(id, l.getRequirement());
            jobW.put(id, props.weightFor(l.getRequirement()));
        }

        boolean coverage = props.isCoverageMode();
        double jaccard = coverage
                ? WeightedJaccardCalculator.computeCoverage(studentW, jobW)
                : WeightedJaccardCalculator.compute(studentW, jobW, props.getExtraSkillPenalty());
        double cosine = computeCosine(studentW, jobW, catalog, coverage);
        double finalScore = props.getAlpha() * jaccard + (1.0 - props.getAlpha()) * cosine;

        List<MatchedSkill> matched = new ArrayList<>();
        List<MissingSkill> missingRequired = new ArrayList<>();
        List<MissingSkill> missingPreferred = new ArrayList<>();
        Comparator<String> byLabel = (a, b) -> {
            Skill sa = catalog.get(a);
            Skill sb = catalog.get(b);
            String la = sa == null ? a : sa.getPreferredLabel();
            String lb = sb == null ? b : sb.getPreferredLabel();
            return la.compareToIgnoreCase(lb);
        };

        List<String> jobIdsSorted = new ArrayList<>(jobReq.keySet());
        jobIdsSorted.sort(byLabel);
        for (String id : jobIdsSorted) {
            Skill skill = catalog.get(id);
            if (skill == null) continue;
            Requirement req = jobReq.get(id);
            if (ownedProf.containsKey(id)) {
                matched.add(new MatchedSkill(id, skill.getPreferredLabel(), req, ownedProf.get(id)));
            } else if (req == Requirement.REQUIRED) {
                missingRequired.add(new MissingSkill(id, skill.getPreferredLabel(), req));
            } else {
                missingPreferred.add(new MissingSkill(id, skill.getPreferredLabel(), req));
            }
        }
        return new ScoreBreakdown(jaccard, cosine, finalScore, matched, missingRequired, missingPreferred);
    }

    private double computeCosine(Map<String, Double> studentW,
                                 Map<String, Double> jobW,
                                 Map<String, Skill> catalog,
                                 boolean coverageMode) {
        if (studentW.isEmpty() || jobW.isEmpty()) {
            return 0.0;
        }
        int dim = inferVectorDim(catalog);
        if (dim <= 0) {
            return 0.0;
        }
        Map<String, Double> studentForVec = coverageMode
                ? intersectKeys(studentW, jobW.keySet())
                : studentW;
        if (studentForVec.isEmpty()) {
            return 0.0;
        }
        double[] studentVec = aggregateVector(studentForVec, catalog, dim);
        double[] jobVec = aggregateVector(jobW, catalog, dim);
        return CosineCalculator.cosine(studentVec, jobVec);
    }

    private static Map<String, Double> intersectKeys(Map<String, Double> source, Set<String> keep) {
        Map<String, Double> out = new HashMap<>();
        for (Map.Entry<String, Double> e : source.entrySet()) {
            if (keep.contains(e.getKey())) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    private double[] aggregateVector(Map<String, Double> weights,
                                     Map<String, Skill> catalog,
                                     int dim) {
        double[] out = new double[dim];
        for (Map.Entry<String, Double> e : weights.entrySet()) {
            Skill skill = catalog.get(e.getKey());
            if (skill == null) continue;
            double[] v = skill.getTfidfVector();
            if (v == null || v.length != dim) continue;
            double w = e.getValue();
            for (int i = 0; i < dim; i++) {
                out[i] += w * v[i];
            }
        }
        return out;
    }

    private static int inferVectorDim(Map<String, Skill> catalog) {
        for (Skill s : catalog.values()) {
            double[] v = s.getTfidfVector();
            if (v != null && v.length > 0) {
                return v.length;
            }
        }
        return 0;
    }

    private Map<String, Skill> loadSkillCatalog(Set<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, Skill> out = new HashMap<>(ids.size() * 2);
        for (Skill s : skillRepository.findAllById(ids)) {
            out.put(s.getId(), s);
        }
        return out;
    }

    private static ScoreBreakdown empty() {
        return new ScoreBreakdown(0.0, 0.0, 0.0, List.of(), List.of(), List.of());
    }
}
