package com.jobskillsmatcher.auditlog.port.rest;

import com.jobskillsmatcher.auditlog.AuditLogService;
import com.jobskillsmatcher.auditlog.impl.jpa.AuditLog;
import com.jobskillsmatcher.auditlog.model.AuditLogFilter;
import com.jobskillsmatcher.auditlog.model.LogAction;
import com.jobskillsmatcher.auditlog.model.LogCategory;
import com.jobskillsmatcher.auditlog.model.LogLevel;
import com.jobskillsmatcher.auditlog.model.LogOutcome;
import com.jobskillsmatcher.user.UserRepository;
import com.jobskillsmatcher.user.impl.jpa.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogsController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @GetMapping
    public Page<AuditLogView> list(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) LogCategory category,
            @RequestParam(required = false) LogLevel level,
            @RequestParam(required = false) LogOutcome outcome,
            @RequestParam(required = false) LogAction action,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        AuditLogFilter filter = new AuditLogFilter(
                email, role, category, level, outcome, action, from, to);

        Page<AuditLog> rows = auditLogService.search(filter,
                PageRequest.of(Math.max(page, 0), safeSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")));

        Map<UUID, String> emailById = resolveEmails(rows.getContent());
        return rows.map(r -> AuditLogView.from(r,
                r.getActorUserId() == null ? null : emailById.get(r.getActorUserId())));
    }

    // Batch lookup for rows whose email snapshot is null.
    private Map<UUID, String> resolveEmails(List<AuditLog> rows) {
        List<UUID> ids = rows.stream()
                .filter(r -> r.getActorEmail() == null && r.getActorUserId() != null)
                .map(AuditLog::getActorUserId)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> map = new HashMap<>();
        for (User u : userRepository.findAllById(ids)) {
            map.put(u.getId(), u.getEmail());
        }
        return map;
    }
}
