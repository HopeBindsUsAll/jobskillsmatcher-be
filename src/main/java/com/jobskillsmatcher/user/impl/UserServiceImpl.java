package com.jobskillsmatcher.user.impl;

import com.jobskillsmatcher.user.AdminProfileRepository;
import com.jobskillsmatcher.user.StudentProfileRepository;
import com.jobskillsmatcher.user.UserMapper;
import com.jobskillsmatcher.user.UserRepository;
import com.jobskillsmatcher.user.UserService;
import com.jobskillsmatcher.user.impl.jpa.AdminProfile;
import com.jobskillsmatcher.user.impl.jpa.StudentProfile;
import com.jobskillsmatcher.user.impl.jpa.User;
import com.jobskillsmatcher.user.model.Role;
import com.jobskillsmatcher.user.port.rest.AdminUserPatchRequest;
import com.jobskillsmatcher.user.port.rest.AdminUserView;
import com.jobskillsmatcher.user.port.rest.MeView;
import com.jobskillsmatcher.user.port.rest.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public MeView loadMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        StudentProfile student = user.getRole() == Role.STUDENT
                ? studentProfileRepository.findById(userId).orElse(null)
                : null;
        AdminProfile admin = user.getRole() == Role.ADMIN
                ? adminProfileRepository.findById(userId).orElse(null)
                : null;
        return userMapper.toMeView(user, student, admin);
    }

    @Override
    @Transactional
    public MeView updateStudentProfile(UUID userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getRole() != Role.STUDENT) {
            throw new ProfileNotEditableException(user.getRole());
        }

        StudentProfile profile = studentProfileRepository.findById(userId)
                .orElseGet(() -> {
                    StudentProfile fresh = new StudentProfile();
                    fresh.setUser(user);
                    return fresh;
                });
        profile.setFullName(trimOrNull(req.fullName()));
        profile.setPreferredRole(trimOrNull(req.preferredRole()));
        profile.setCountry(upperOrNull(req.country()));
        profile.setCity(trimOrNull(req.city()));
        profile.setRemotePreference(req.remotePreference());
        studentProfileRepository.save(profile);

        return loadMe(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserView> listUsers(Role role, Boolean enabled, String query, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        String q = query == null ? "" : query.trim();
        Page<User> rows = userRepository.filter(role, enabled, q, PageRequest.of(safePage, safeSize));
        Map<UUID, String> displayNames = loadDisplayNames(rows.getContent());
        return rows.map(u -> userMapper.toAdminUserView(u, displayNames.get(u.getId())));
    }

    @Override
    @Transactional
    public AdminUserView patchUser(UUID userId, AdminUserPatchRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (req.enabled() != null) {
            user.setEnabled(req.enabled());
        }
        userRepository.save(user);
        Map<UUID, String> displayNames = loadDisplayNames(List.of(user));
        return userMapper.toAdminUserView(user, displayNames.get(user.getId()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(retryFor = DataIntegrityViolationException.class, maxAttempts = 3, backoff = @Backoff(delay = 50))
    public UUID importByOAuth2(OAuth2User principal) {
        String email = principal.getAttribute("email");
        String googleSub = principal.getAttribute("sub");
        String fullName = principal.getAttribute("name");
        if (email == null || googleSub == null) {
            throw new IllegalStateException("OAuth2 principal missing email or sub");
        }
        String emailLower = email.toLowerCase();
        User user = userRepository.findByGoogleSub(googleSub)
                .or(() -> userRepository.findByEmailIgnoreCase(emailLower))
                .orElseGet(User::new);
        boolean isNew = user.getId() == null;
        userMapper.update(user, principal);
        User saved = userRepository.saveAndFlush(user);
        if (isNew && saved.getRole() == Role.STUDENT) {
            StudentProfile profile = new StudentProfile();
            profile.setUser(saved);
            profile.setFullName(fullName);
            studentProfileRepository.save(profile);
        }
        return saved.getId();
    }

    private Map<UUID, String> loadDisplayNames(List<User> users) {
        if (users.isEmpty()) return Map.of();
        Map<UUID, String> out = new HashMap<>();
        List<UUID> studentIds = users.stream()
                .filter(u -> u.getRole() == Role.STUDENT).map(User::getId).toList();
        List<UUID> adminIds = users.stream()
                .filter(u -> u.getRole() == Role.ADMIN).map(User::getId).toList();
        if (!studentIds.isEmpty()) {
            for (StudentProfile s : studentProfileRepository.findAllById(studentIds)) {
                out.put(s.getUserId(), s.getFullName());
            }
        }
        if (!adminIds.isEmpty()) {
            for (AdminProfile a : adminProfileRepository.findAllById(adminIds)) {
                out.put(a.getUserId(), a.getDisplayName());
            }
        }
        return out;
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String upperOrNull(String value) {
        String t = trimOrNull(value);
        return t == null ? null : t.toUpperCase();
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(UUID userId) {
            super("User not found: " + userId);
        }
    }

    public static class ProfileNotEditableException extends RuntimeException {
        public ProfileNotEditableException(Role role) {
            super("Profile editing not supported for role: " + role);
        }
    }
}
