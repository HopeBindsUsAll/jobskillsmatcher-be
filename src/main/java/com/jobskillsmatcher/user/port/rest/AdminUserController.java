package com.jobskillsmatcher.user.port.rest;

import com.jobskillsmatcher.user.UserService;
import com.jobskillsmatcher.user.model.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin · Users", description = "List and deactivate user accounts.")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List users", description = "Paginated user list filtered by role and enabled flag.")
    public Page<AdminUserView> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.listUsers(role, enabled, query, page, size);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update user enabled flag",
            description = "Toggle the user's enabled state. A disabled user cannot log in.")
    public AdminUserView patch(@PathVariable UUID id, @RequestBody AdminUserPatchRequest req) {
        return userService.patchUser(id, req);
    }
}
