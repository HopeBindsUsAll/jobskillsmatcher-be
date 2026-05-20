package com.jobskillsmatcher.cv.impl;

import com.jobskillsmatcher.cv.CvScanService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobskillsmatcher.cv.impl.jpa.CvUpload;
import com.jobskillsmatcher.cv.CvUploadRepository;
import com.jobskillsmatcher.cv.port.rest.CvScanResultView;
import com.jobskillsmatcher.cv.port.rest.CvSkillView;
import com.jobskillsmatcher.cv.port.rest.CvUploadSummaryView;
import com.jobskillsmatcher.ingest.impl.JobSkillExtractor;
import com.jobskillsmatcher.ingest.impl.groq.GroqSkillEnricher;
import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.JobRepository;
import com.jobskillsmatcher.job.impl.jpa.JobSkill;
import com.jobskillsmatcher.job.JobSkillRepository;
import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import com.jobskillsmatcher.user.impl.jpa.StudentProfile;
import com.jobskillsmatcher.user.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CvScanServiceImpl implements CvScanService {

    private static final int PREFERRED_ROLE_POOL_SIZE = 60;

    private final CvProperties cvProperties;
    private final CvTextExtractor textExtractor;
    private final JobSkillExtractor skillExtractor;
    private final GroqSkillEnricher groqSkillEnricher;
    private final SkillRepository skillRepository;
    private final CvUploadRepository cvUploadRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<CvUploadSummaryView> listForStudent(UUID studentId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        return cvUploadRepository
                .findByStudentIdOrderByUploadedAtDesc(studentId, PageRequest.of(safePage, safeSize))
                .map(this::summaryOf);
    }

    @Transactional(readOnly = true)
    public CvScanResultView getForStudent(UUID studentId, UUID uploadId) {
        CvUpload entity = cvUploadRepository.findById(uploadId)
                .orElseThrow(() -> new CvUploadNotFoundException(uploadId));
        if (!entity.getStudentId().equals(studentId)) {
            // Don't leak existence across users.
            throw new CvUploadNotFoundException(uploadId);
        }
        Set<String> extractedIds = deserialiseSkillIds(entity.getExtractedSkills());

        StudentProfile profile = studentProfileRepository.findById(studentId).orElse(null);
        String preferredRole = profile == null ? null : profile.getPreferredRole();
        String country = profile == null ? null : profile.getCountry();

        // Recompute against the current preferred role so old scans stay useful after a role change.
        Set<String> roleRequiredIds = preferredRole == null || preferredRole.isBlank()
                ? Set.of()
                : requiredSkillsForRole(preferredRole, country);
        Set<String> missingIds = new LinkedHashSet<>(roleRequiredIds);
        missingIds.removeAll(extractedIds);

        Set<String> allReferenced = new HashSet<>(extractedIds);
        allReferenced.addAll(missingIds);
        Map<String, String> labelById = loadLabels(allReferenced);
        return new CvScanResultView(
                entity.getId(),
                entity.getFilename(),
                entity.getSizeBytes(),
                entity.getContentType(),
                entity.getUploadedAt(),
                toSortedView(extractedIds, labelById),
                toSortedView(missingIds, labelById),
                preferredRole);
    }

    private CvUploadSummaryView summaryOf(CvUpload entity) {
        Set<String> ids = deserialiseSkillIds(entity.getExtractedSkills());
        return new CvUploadSummaryView(
                entity.getId(),
                entity.getFilename(),
                entity.getSizeBytes(),
                entity.getContentType(),
                entity.getUploadedAt(),
                ids.size());
    }

    private Set<String> deserialiseSkillIds(String json) {
        if (json == null || json.isBlank()) return Set.of();
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashSet<String>>() { });
        } catch (Exception ex) {
            return Set.of();
        }
    }

    @Transactional
    public CvScanResultView scan(UUID studentId, MultipartFile file) {
        validate(file);

        String text;
        try {
            text = textExtractor.extract(file.getInputStream());
        } catch (IOException ex) {
            throw new CvUploadException("Could not read upload: " + ex.getMessage());
        }

        Map<String, Requirement> algo = skillExtractor.extract(text);
        Map<String, Requirement> matched = groqSkillEnricher.enrich(text, algo);
        Set<String> extractedIds = matched.keySet();

        StudentProfile profile = studentProfileRepository.findById(studentId).orElse(null);
        String preferredRole = profile == null ? null : profile.getPreferredRole();
        String country = profile == null ? null : profile.getCountry();

        Set<String> roleRequiredIds = preferredRole == null || preferredRole.isBlank()
                ? Set.of()
                : requiredSkillsForRole(preferredRole, country);

        Set<String> missingIds = new LinkedHashSet<>(roleRequiredIds);
        missingIds.removeAll(extractedIds);

        Set<String> allReferenced = new HashSet<>(extractedIds);
        allReferenced.addAll(missingIds);
        Map<String, String> labelById = loadLabels(allReferenced);

        List<CvSkillView> extractedView = toSortedView(extractedIds, labelById);
        List<CvSkillView> missingView = toSortedView(missingIds, labelById);

        CvUpload record = new CvUpload();
        record.setId(UUID.randomUUID());
        record.setStudentId(studentId);
        record.setFilename(safeName(file.getOriginalFilename()));
        record.setContentType(file.getContentType() == null ? "" : file.getContentType());
        record.setSizeBytes(file.getSize());
        record.setExtractedSkills(serialise(extractedIds));
        cvUploadRepository.save(record);

        return new CvScanResultView(
                record.getId(),
                record.getFilename(),
                record.getSizeBytes(),
                record.getContentType(),
                record.getUploadedAt(),
                extractedView,
                missingView,
                preferredRole);
    }

    private Set<String> requiredSkillsForRole(String role, String country) {
        List<Job> jobs = jobRepository.findByPreferredRole(
                role.trim(),
                country == null ? "" : country.trim().toUpperCase(Locale.ROOT),
                PageRequest.of(0, PREFERRED_ROLE_POOL_SIZE));
        if (jobs.isEmpty()) {
            // Fall back to all-country so brand-new profiles still get suggestions.
            jobs = jobRepository.findByPreferredRole(role.trim(), "",
                    PageRequest.of(0, PREFERRED_ROLE_POOL_SIZE));
        }
        if (jobs.isEmpty()) return Set.of();
        List<UUID> jobIds = jobs.stream().map(Job::getId).toList();
        List<JobSkill> links = jobSkillRepository.findAllByIdJobIdIn(jobIds);
        Set<String> required = new HashSet<>();
        for (JobSkill link : links) {
            if (link.getRequirement() == Requirement.REQUIRED) {
                required.add(link.getId().getSkillId());
            }
        }
        return required;
    }

    private Map<String, String> loadLabels(Set<String> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<String, String> out = new HashMap<>(ids.size() * 2);
        for (Skill s : skillRepository.findAllById(ids)) {
            out.put(s.getId(), s.getPreferredLabel());
        }
        return out;
    }

    private List<CvSkillView> toSortedView(Set<String> ids, Map<String, String> labels) {
        List<CvSkillView> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            out.add(new CvSkillView(id, labels.getOrDefault(id, id)));
        }
        out.sort(Comparator.comparing(CvSkillView::preferredLabel, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CvUploadException("Upload is empty.");
        }
        if (file.getSize() > cvProperties.getMaxBytes()) {
            throw new CvUploadException("CV exceeds maximum size of "
                    + cvProperties.getMaxBytes() + " bytes.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !cvProperties.getAcceptedContentTypes().contains(contentType)) {
            throw new CvUploadException("Unsupported content type: " + contentType
                    + ". Accepted: " + cvProperties.getAcceptedContentTypes());
        }
    }

    private String serialise(Set<String> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "cv";
        return name.length() > 500 ? name.substring(0, 500) : name;
    }

    public static class CvUploadException extends RuntimeException {
        public CvUploadException(String message) {
            super(message);
        }
    }

    public static class CvUploadNotFoundException extends RuntimeException {
        public CvUploadNotFoundException(UUID id) {
            super("CV upload not found: " + id);
        }
    }
}
