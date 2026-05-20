package com.jobskillsmatcher.auditlog.impl.web;

import com.jobskillsmatcher.auditlog.AuditLogService;
import com.jobskillsmatcher.auditlog.model.AuditLogEntry;
import com.jobskillsmatcher.auditlog.model.LogLevel;
import com.jobskillsmatcher.auditlog.model.LogOutcome;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.UUID;

// Audits every mutating /api request. Auth flows are audited separately in AuthServiceImpl.
@Component
@RequiredArgsConstructor
public class AuditLogInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID = "requestId";
    private static final String START_NANOS = AuditLogInterceptor.class.getName() + ".start";
    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final AuditLogService auditLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_NANOS, System.nanoTime());
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID, requestId);
        MDC.put(REQUEST_ID, requestId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            if (!shouldCapture(request)) {
                return;
            }
            int status = response.getStatus();
            Long durationMs = null;
            Object start = request.getAttribute(START_NANOS);
            if (start instanceof Long s) {
                durationMs = (System.nanoTime() - s) / 1_000_000L;
            }
            LogLevel level = status >= 500 ? LogLevel.ERROR
                    : status >= 400 ? LogLevel.WARN : LogLevel.INFO;
            LogOutcome outcome = status >= 400 ? LogOutcome.FAILURE : LogOutcome.SUCCESS;

            UUID actorUserId = null;
            String actorRole = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken)) {
                actorUserId = parseUuid(String.valueOf(auth.getPrincipal()));
                actorRole = firstRole(auth);
            }
            String message = ex != null ? ex.getClass().getSimpleName() + ": " + ex.getMessage()
                    : null;

            auditLogService.record(AuditLogEntry.httpMutation(
                    level, outcome, actorUserId, actorRole,
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    clientIp(request),
                    request.getHeader("User-Agent"),
                    durationMs,
                    (String) request.getAttribute(REQUEST_ID),
                    message));
        } finally {
            MDC.remove(REQUEST_ID);
        }
    }

    private boolean shouldCapture(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return MUTATING.contains(request.getMethod())
                && uri != null
                && uri.startsWith("/api/")
                && !uri.startsWith("/api/auth/");
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String firstRole(Authentication auth) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            String role = a.getAuthority();
            return role != null && role.startsWith("ROLE_") ? role.substring(5) : role;
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
