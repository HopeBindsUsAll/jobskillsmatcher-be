package com.jobskillsmatcher.auth.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobskillsmatcher.auditlog.AuditLogService;
import com.jobskillsmatcher.auditlog.model.AuditLogEntry;
import com.jobskillsmatcher.auditlog.model.LogAction;
import com.jobskillsmatcher.auth.AuthMapper;
import com.jobskillsmatcher.auth.AuthRepository;
import com.jobskillsmatcher.auth.impl.jpa.Auth;
import com.jobskillsmatcher.auth.model.AuthToken;
import com.jobskillsmatcher.user.StudentProfileRepository;
import com.jobskillsmatcher.user.UserRepository;
import com.jobskillsmatcher.user.UserService;
import com.jobskillsmatcher.user.UserTestDataFactory;
import com.jobskillsmatcher.user.impl.jpa.StudentProfile;
import com.jobskillsmatcher.user.impl.jpa.User;
import com.jobskillsmatcher.user.model.Role;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private static RSAKey rsaKey;
    private static JWSSigner jwsSigner;
    private static RSASSAVerifier rsassaVerifier;

    private AuthRepository authRepository;
    private ObjectMapper objectMapper;
    private AuthMapper authMapper;
    private UserService userService;
    private UserRepository userRepository;
    private StudentProfileRepository studentProfileRepository;
    private PasswordEncoder passwordEncoder;
    private AuditLogService auditLogService;
    private AuthServiceImpl authService;

    @BeforeAll
    static void generateKey() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("test").generate();
        jwsSigner = new RSASSASigner(rsaKey);
        rsassaVerifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
    }

    @BeforeEach
    void setUp() {
        authRepository = mock(AuthRepository.class);
        objectMapper = new ObjectMapper();
        authMapper = mock(AuthMapper.class);
        userService = mock(UserService.class);
        userRepository = mock(UserRepository.class);
        studentProfileRepository = mock(StudentProfileRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditLogService = mock(AuditLogService.class);
        authService = new AuthServiceImpl(authRepository, objectMapper, authMapper, userService,
                userRepository, studentProfileRepository, passwordEncoder, jwsSigner, rsassaVerifier,
                auditLogService);
        ReflectionTestUtils.setField(authService, "lifetime", Duration.ofHours(1));
        ReflectionTestUtils.setField(authService, "spaRedirect", "http://localhost/oauth/callback");
    }

    @Test
    void successRegisterCreatesUserStudentProfileAndToken() {
        when(userRepository.existsByEmailIgnoreCase("student@test.io")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn(UserTestDataFactory.PASSWORD_HASH);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UserTestDataFactory.STUDENT_ID);
            return u;
        });
        when(authMapper.fromUser(any(User.class))).thenAnswer(inv -> authFor(inv.getArgument(0)));
        when(authRepository.save(any(Auth.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthToken token = authService.register("  Student@Test.io ", "Password123", "Test Student");

        assertThat(token.getAccessToken()).isNotBlank();
        assertThat(token.getRefreshToken()).isNotBlank();
        assertThat(token.getExpiresIn()).isEqualTo(Duration.ofHours(1).toMillis());

        verify(userRepository).saveAndFlush(argThat(u ->
                u.getEmail().equals("student@test.io")
                        && u.getRole() == Role.STUDENT
                        && u.isEnabled()
                        && UserTestDataFactory.PASSWORD_HASH.equals(u.getPasswordHash())));
        verify(studentProfileRepository).save(argThat((StudentProfile p) ->
                "Test Student".equals(p.getFullName())));
        verify(auditLogService).record(argThat((AuditLogEntry e) ->
                e.action() == LogAction.REGISTER && "student@test.io".equals(e.actorEmail())));
    }

    @Test
    void failedRegisterThrowsWhenEmailBlank() {
        assertThatThrownBy(() -> authService.register("   ", "pw", "Name"))
                .isInstanceOf(BadCredentialsException.class);
        verify(userRepository, never()).saveAndFlush(any());
        verify(auditLogService, never()).record(any());
    }

    @Test
    void failedRegisterThrowsEmailAlreadyUsedException() {
        when(userRepository.existsByEmailIgnoreCase("student@test.io")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("student@test.io", "pw", "Name"))
                .isInstanceOf(AuthServiceImpl.EmailAlreadyUsedException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void failedLoginThrowsBadCredentialsForInvalidEmail() {
        when(userRepository.findByEmailIgnoreCase("ghost@test.io")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost@test.io", "pw"))
                .isInstanceOf(BadCredentialsException.class);
        verify(auditLogService).record(argThat((AuditLogEntry e) ->
                e.action() == LogAction.LOGIN_FAILED && "ghost@test.io".equals(e.actorEmail())));
    }

    @Test
    void failedLoginThrowsBadCredentialsForWrongPassword() {
        User student = UserTestDataFactory.student();
        when(userRepository.findByEmailIgnoreCase("student@test.io")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("wrong", student.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login("student@test.io", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
        verify(auditLogService).record(argThat((AuditLogEntry e) ->
                e.action() == LogAction.LOGIN_FAILED));
        verify(authRepository, never()).save(any());
    }

    @Test
    void failedLoginThrowsDisabledExceptionWhenUserDisabled() {
        when(userRepository.findByEmailIgnoreCase("student@test.io"))
                .thenReturn(Optional.of(UserTestDataFactory.disabledStudent()));

        assertThatThrownBy(() -> authService.login("student@test.io", "Password123"))
                .isInstanceOf(DisabledException.class);
        verify(auditLogService).record(argThat((AuditLogEntry e) ->
                e.action() == LogAction.LOGIN_FAILED));
    }

    @Test
    void successRefreshTokenRegeneratesJti() throws Exception {
        User student = UserTestDataFactory.student();
        Auth existing = authFor(student);
        UUID originalJti = existing.getJti();
        when(authRepository.findByIdAndJti(existing.getId(), originalJti))
                .thenReturn(Optional.of(existing));
        when(authRepository.save(any(Auth.class))).thenAnswer(inv -> inv.getArgument(0));

        String accessToken = signedJwtFor(student, originalJti);

        AuthToken refreshed = authService.refreshToken(existing.getId().toString(), accessToken);

        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(existing.getJti()).isNotEqualTo(originalJti);
        verify(authRepository).save(existing);
        verify(auditLogService).record(argThat((AuditLogEntry e) ->
                e.action() == LogAction.TOKEN_REFRESH));
    }

    @Test
    void failedRefreshThrowsBadCredentialsWhenRecordNotFound() throws Exception {
        User student = UserTestDataFactory.student();
        UUID jti = UUID.randomUUID();
        when(authRepository.findByIdAndJti(any(UUID.class), eq(jti))).thenReturn(Optional.empty());

        String accessToken = signedJwtFor(student, jti);

        assertThatThrownBy(() -> authService.refreshToken(UUID.randomUUID().toString(), accessToken))
                .isInstanceOf(BadCredentialsException.class);
        verify(authRepository, never()).save(any());
    }

    @Test
    void coverageLogoutDeletesAuthRecordAndRecordsAudit() throws Exception {
        User student = UserTestDataFactory.student();
        Auth auth = authFor(student);
        Authentication authentication = mock(UsernamePasswordAuthenticationToken.class);
        when(authentication.getDetails()).thenReturn((Supplier<Auth>) () -> auth);
        when(authentication.getPrincipal()).thenReturn(student.getId().toString());
        jakarta.servlet.http.HttpServletResponse rp = mock(jakarta.servlet.http.HttpServletResponse.class);
        java.io.ByteArrayOutputStream sink = new java.io.ByteArrayOutputStream();
        when(rp.getOutputStream()).thenReturn(new jakarta.servlet.ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(jakarta.servlet.WriteListener l) { }
            @Override public void write(int b) { sink.write(b); }
        });

        authService.logout(mock(jakarta.servlet.http.HttpServletRequest.class), rp, authentication);

        verify(authRepository).delete(auth);
        verify(auditLogService).record(argThat((AuditLogEntry e) ->
                e.action() == LogAction.LOGOUT && student.getEmail().equals(e.actorEmail())));
    }

    private static Auth authFor(User user) {
        Auth auth = new Auth();
        auth.setId(UUID.randomUUID());
        auth.setUser(user);
        auth.setJti(UUID.randomUUID());
        return auth;
    }

    private static String signedJwtFor(User user, UUID jti) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(jti.toString())
                .subject(user.getId().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofHours(1))))
                .audience(java.util.List.of(user.getRole().name()))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), claims);
        jwt.sign(jwsSigner);
        return jwt.serialize();
    }
}
