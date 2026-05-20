package com.jobskillsmatcher.auth.port.rest;

import com.jobskillsmatcher.auth.AuthService;
import com.jobskillsmatcher.auth.model.AuthToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Email/password and OAuth2-issued JWT lifecycle")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Create a STUDENT account and receive a JWT pair.")
    public AuthToken register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.email(), request.password(), request.fullName());
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Exchange email + password for a JWT pair.")
    public AuthToken login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT", description = "Rotate the JWT given a valid refresh token and the current access token.")
    public AuthToken refresh(@Valid @RequestBody RefreshRequest request) throws ParseException {
        return authService.refreshToken(request.refreshToken(), request.accessToken());
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 200) String fullName
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 100) String password
    ) {
    }

    public record RefreshRequest(
            @NotBlank String accessToken,
            @NotBlank String refreshToken
    ) {
    }
}
