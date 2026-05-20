package com.jobskillsmatcher.user.port.rest;

import com.jobskillsmatcher.context.security.CurrentUser;
import com.jobskillsmatcher.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Me", description = "The current user's profile and preferences.")
public class MeController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Current user", description = "Returns the authenticated user and their profile.")
    public MeView me() {
        return userService.loadMe(CurrentUser.requireId());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update student profile",
            description = "Update preferred role, country, city and remote preference.")
    public MeView updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        return userService.updateStudentProfile(CurrentUser.requireId(), req);
    }
}
