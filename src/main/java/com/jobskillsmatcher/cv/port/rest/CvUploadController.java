package com.jobskillsmatcher.cv.port.rest;

import com.jobskillsmatcher.context.security.CurrentUser;
import com.jobskillsmatcher.cv.CvScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/me/cv")
@RequiredArgsConstructor
@Tag(name = "CV Scan", description = "Tika-based CV upload that returns extracted ESCO skills.")
public class CvUploadController {

    private final CvScanService cvScanService;

    @PostMapping
    @Operation(summary = "Scan CV",
            description = "Upload a PDF or DOCX (≤10 MB). Returns detected skills and skills missing for the preferred role. Does not mutate the student's profile.")
    public CvScanResultView upload(@RequestParam("file") MultipartFile file) {
        return cvScanService.scan(CurrentUser.requireId(), file);
    }

    @GetMapping
    @Operation(summary = "List my CV scans",
            description = "Paginated history of past CV uploads, newest first.")
    public Page<CvUploadSummaryView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return cvScanService.listForStudent(CurrentUser.requireId(), page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a past CV scan",
            description = "Returns extracted skills + missing-for-preferred-role section recomputed against the student's current preferred role.")
    public CvScanResultView get(@PathVariable("id") UUID id) {
        return cvScanService.getForStudent(CurrentUser.requireId(), id);
    }
}
