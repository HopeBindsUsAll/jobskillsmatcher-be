package com.jobskillsmatcher.user.impl;

import com.jobskillsmatcher.user.UserService;

import com.jobskillsmatcher.user.UserTestDataFactory;
import com.jobskillsmatcher.user.AdminProfileRepository;
import com.jobskillsmatcher.user.StudentProfileRepository;
import com.jobskillsmatcher.user.impl.jpa.User;
import com.jobskillsmatcher.user.UserRepository;
import com.jobskillsmatcher.user.model.Role;
import com.jobskillsmatcher.user.port.rest.AdminUserPatchRequest;
import com.jobskillsmatcher.user.port.rest.AdminUserView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceAdminTest {

    @Mock
    UserRepository userRepository;

    @Mock
    StudentProfileRepository studentProfileRepository;

    @Mock
    AdminProfileRepository adminProfileRepository;

    @Spy
    UserMappers userMapper = new UserMappers();

    @InjectMocks
    UserServiceImpl userService;

    @Test
    void listUsersClampsPageSizeAndForwardsFilters() {
        User student = UserTestDataFactory.student();
        when(userRepository.filter(eq(Role.STUDENT), eq(Boolean.TRUE), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(student), PageRequest.of(0, 50), 1));
        when(studentProfileRepository.findAllById(any()))
                .thenReturn(List.of(UserTestDataFactory.studentProfile()));

        Page<AdminUserView> page = userService.listUsers(Role.STUDENT, true, "", 0, 9999);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).email()).isEqualTo(UserTestDataFactory.STUDENT_EMAIL);
        assertThat(page.getContent().get(0).displayName()).isEqualTo("Test Student");
        // Pagination size capped at 100.
        verify(userRepository).filter(eq(Role.STUDENT), eq(Boolean.TRUE), eq(""),
                eq(PageRequest.of(0, 100)));
    }

    @Test
    void patchUserDisablesAccount() {
        User student = UserTestDataFactory.student();
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentProfileRepository.findAllById(any()))
                .thenReturn(List.of(UserTestDataFactory.studentProfile()));

        AdminUserView updated = userService.patchUser(student.getId(), new AdminUserPatchRequest(false));

        assertThat(updated.enabled()).isFalse();
        assertThat(student.isEnabled()).isFalse();
    }

    @Test
    void patchUserNullEnabledIsNoop() {
        User admin = UserTestDataFactory.admin();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adminProfileRepository.findAllById(any()))
                .thenReturn(List.of(UserTestDataFactory.adminProfile()));

        boolean wasEnabled = admin.isEnabled();
        AdminUserView updated = userService.patchUser(admin.getId(), new AdminUserPatchRequest(null));

        assertThat(updated.enabled()).isEqualTo(wasEnabled);
    }

    @Test
    void patchUserMissingIdRaises() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.patchUser(missing, new AdminUserPatchRequest(true)))
                .isInstanceOf(UserServiceImpl.UserNotFoundException.class);
    }
}
