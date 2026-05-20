package com.jobskillsmatcher.auth.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobskillsmatcher.auditlog.AuditLogService;
import com.jobskillsmatcher.auditlog.model.AuditLogEntry;
import com.jobskillsmatcher.auditlog.model.LogAction;
import com.jobskillsmatcher.auditlog.model.LogLevel;
import com.jobskillsmatcher.auditlog.model.LogOutcome;
import com.jobskillsmatcher.auth.AuthMapper;
import com.jobskillsmatcher.auth.AuthRepository;
import com.jobskillsmatcher.auth.AuthService;
import com.jobskillsmatcher.auth.impl.jpa.Auth;
import com.jobskillsmatcher.auth.model.AuthToken;
import com.jobskillsmatcher.user.StudentProfileRepository;
import com.jobskillsmatcher.user.UserRepository;
import com.jobskillsmatcher.user.UserService;
import com.jobskillsmatcher.user.impl.jpa.StudentProfile;
import com.jobskillsmatcher.user.impl.jpa.User;
import com.jobskillsmatcher.user.model.Role;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BEARER = "Bearer ";

    private final AuthRepository authRepository;
    private final ObjectMapper objectMapper;
    private final AuthMapper authMapper;
    private final UserService userService;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWSSigner jwsSigner;
    private final RSASSAVerifier rsassaVerifier;
    private final AuditLogService auditLogService;

    @Value("${spring.security.jwt.lifetime:PT1H}")
    private Duration lifetime;

    @Value("${jobskillsmatcher.oauth.spa-redirect:http://localhost:5173/oauth/callback}")
    private String spaRedirect;

    @Override
    public void jwtFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
        jwtFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
    }

    @Override
    public void jwtFilter(HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
        try {
            resolveJwt(servletRequest, servletResponse);
        } catch (ParseException e) {
            throw new BadCredentialsException("Invalid token");
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private SignedJWT decodeToken(String token) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            if (signedJwt.verify(rsassaVerifier)) {
                return signedJwt;
            } else {
                throw new BadCredentialsException("Invalid token");
            }
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid token");
        }
    }

    private void resolveJwt(HttpServletRequest rq, HttpServletResponse rp) throws ParseException {
        String header = rq.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            return;
        }
        SignedJWT jwt = decodeToken(header.substring(BEARER.length()));

        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        if (claims.getExpirationTime() == null || !claims.getExpirationTime().toInstant().isAfter(Instant.now())) {
            failureHandle(rp, new CredentialsExpiredException("Token expired"));
            return;
        }

        Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(
                claims.getAudience().stream().map(role -> "ROLE_" + role).toArray(String[]::new));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                claims.getSubject(), jwt.serialize(), authorities);

        authentication.setDetails((Supplier<Auth>) () -> authRepository
                .findByIdAndJti(UUID.fromString(claims.getSubject()), UUID.fromString(claims.getJWTID()))
                .orElseThrow(() -> new BadCredentialsException("Auth record not found")));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Override
    public void authenticate(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oAuth2AuthenticationToken) {
            authenticateOAuth2Token(response, oAuth2AuthenticationToken);
        } else if (authentication instanceof UsernamePasswordAuthenticationToken upat) {
            authenticateUsernamePasswordToken(response, upat);
        }
    }

    private void authenticateOAuth2Token(HttpServletResponse rp, OAuth2AuthenticationToken authenticationToken) {
        UUID userId = userService.importByOAuth2(authenticationToken.getPrincipal());
        Auth auth = authMapper.fromUser(userId);
        if (!auth.getUser().isEnabled()) {
            throw new DisabledException("User is disabled");
        }
        Auth saved = authRepository.save(auth);
        AuthToken token = createToken(saved);
        try {
            rp.sendRedirect(buildSpaRedirect(token));
        } catch (IOException e) {
            throw new AuthenticationServiceException("Failed to redirect after OAuth2 login", e);
        }
    }

    private String buildSpaRedirect(AuthToken token) {
        return spaRedirect
                + "#access_token=" + URLEncoder.encode(token.getAccessToken(), StandardCharsets.UTF_8)
                + "&refresh_token=" + URLEncoder.encode(token.getRefreshToken(), StandardCharsets.UTF_8)
                + "&expires_in=" + token.getExpiresIn();
    }

    private void authenticateUsernamePasswordToken(HttpServletResponse response, UsernamePasswordAuthenticationToken authentication) {
        Auth auth = authMapper.fromUser((User) authentication.getPrincipal());
        if (!auth.getUser().isEnabled()) {
            throw new DisabledException("User is disabled");
        }
        handle(response, createToken(authRepository.save(auth)));
    }

    private AuthToken createToken(Auth auth) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(auth.getJti().toString())
                .subject(auth.getUser().getId().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(lifetime)))
                .audience(java.util.List.of(auth.getUser().getRole().name()))
                .build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .build();
        SignedJWT signedJWT = new SignedJWT(header, claims);
        try {
            signedJWT.sign(jwsSigner);
        } catch (JOSEException e) {
            throw new AuthenticationServiceException("Failed to sign token");
        }
        return new AuthToken(signedJWT.serialize(), auth.getId().toString(), lifetime.toMillis());
    }

    @Override
    public AuthToken register(String email, String password, String fullName) {
        String normalisedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalisedEmail.isEmpty()) {
            throw new BadCredentialsException("Email is required");
        }
        if (userRepository.existsByEmailIgnoreCase(normalisedEmail)) {
            throw new EmailAlreadyUsedException(normalisedEmail);
        }
        User user = new User();
        user.setEmail(normalisedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);
        User saved = userRepository.saveAndFlush(user);

        StudentProfile profile = new StudentProfile();
        profile.setUser(saved);
        profile.setFullName(fullName);
        studentProfileRepository.save(profile);

        AuthToken token = issueToken(saved);
        auditLogService.record(AuditLogEntry.audit(LogAction.REGISTER, LogLevel.INFO,
                LogOutcome.SUCCESS, saved.getId(), saved.getEmail(), saved.getRole().name(),
                "User registered"));
        return token;
    }

    @Override
    public AuthToken login(String email, String password) {
        String normalised = email == null ? "" : email.trim().toLowerCase();
        try {
            User user = userRepository.findByEmailIgnoreCase(normalised)
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
            if (!user.isEnabled()) {
                throw new DisabledException("Account is disabled");
            }
            if (user.getPasswordHash() == null
                    || !passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new BadCredentialsException("Invalid email or password");
            }
            AuthToken token = issueToken(user);
            auditLogService.record(AuditLogEntry.audit(LogAction.LOGIN_SUCCESS, LogLevel.INFO,
                    LogOutcome.SUCCESS, user.getId(), user.getEmail(), user.getRole().name(),
                    "Login success"));
            return token;
        } catch (RuntimeException ex) {
            auditLogService.record(AuditLogEntry.audit(LogAction.LOGIN_FAILED, LogLevel.WARN,
                    LogOutcome.FAILURE, null, normalised, null,
                    ex.getClass().getSimpleName() + ": " + ex.getMessage()));
            throw ex;
        }
    }

    private AuthToken issueToken(User user) {
        Auth auth = authMapper.fromUser(user);
        return createToken(authRepository.save(auth));
    }

    @Override
    public AuthToken refreshToken(String refreshToken, String accessToken) throws ParseException {
        SignedJWT jwt = decodeToken(accessToken.startsWith(BEARER) ? accessToken.substring(BEARER.length()) : accessToken);
        Auth auth = authRepository.findByIdAndJti(
                        UUID.fromString(refreshToken),
                        UUID.fromString(jwt.getJWTClaimsSet().getJWTID()))
                .orElseThrow(() -> new BadCredentialsException("Auth record not found"));

        if (!auth.getUser().isEnabled()) {
            throw new DisabledException("Cannot refresh: user is disabled");
        }

        auth.setJti(UUID.randomUUID());
        authRepository.save(auth);

        log.info("Refreshed JWT for '{}'", auth.getUser().getEmail());
        auditLogService.record(AuditLogEntry.audit(LogAction.TOKEN_REFRESH, LogLevel.INFO,
                LogOutcome.SUCCESS, auth.getUser().getId(), auth.getUser().getEmail(),
                auth.getUser().getRole().name(), "Token refreshed"));
        return createToken(auth);
    }

    @Override
    public void failure(HttpServletRequest rq, HttpServletResponse rp, RuntimeException exception) {
        failureHandle(rp, exception);
    }

    private void failureHandle(HttpServletResponse rp, RuntimeException exception) {
        if (exception instanceof AccessDeniedException) {
            handle(rp, ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getLocalizedMessage()));
        } else {
            handle(rp, ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getLocalizedMessage()));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void logout(HttpServletRequest rq, HttpServletResponse rp, Authentication authentication) {
        try {
            if (authentication == null) {
                handle(rp, ProblemDetail.forStatusAndDetail(HttpStatus.OK, "No active session"));
                return;
            }
            Auth auth = ((Supplier<Auth>) authentication.getDetails()).get();
            if (auth != null && auth.getUser().getId().toString().equals(authentication.getPrincipal())) {
                authRepository.delete(auth);
                log.info("User '{}' logged out", authentication.getPrincipal());
                auditLogService.record(AuditLogEntry.audit(LogAction.LOGOUT, LogLevel.INFO,
                        LogOutcome.SUCCESS, auth.getUser().getId(), auth.getUser().getEmail(),
                        auth.getUser().getRole().name(), "User logged out"));
            } else {
                failureHandle(rp, new AccessDeniedException("User does not match auth record"));
                return;
            }
            handle(rp, ProblemDetail.forStatusAndDetail(HttpStatus.OK, "Successful logout"));
        } catch (RuntimeException e) {
            failureHandle(rp, e);
        }
    }

    public static class EmailAlreadyUsedException extends RuntimeException {
        public EmailAlreadyUsedException(String email) {
            super("Email already registered: " + email);
        }
    }

    @Override
    public void handle(HttpServletResponse rp, Object payload) {
        rp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (payload instanceof ProblemDetail pd) {
            rp.setStatus(pd.getStatus());
        }
        try (var outputStream = rp.getOutputStream()) {
            objectMapper.writeValue(outputStream, payload);
        } catch (IOException e) {
            throw new AccessDeniedException(e.getLocalizedMessage());
        }
    }
}
