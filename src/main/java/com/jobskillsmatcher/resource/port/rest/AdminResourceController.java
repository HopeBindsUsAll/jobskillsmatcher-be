package com.jobskillsmatcher.resource.port.rest;

import com.jobskillsmatcher.resource.LearningResourceService;
import com.jobskillsmatcher.resource.model.ResourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/resources")
@RequiredArgsConstructor
@Tag(name = "Admin · Resources", description = "CRUD over the learning-resource catalogue.")
public class AdminResourceController {

    private final LearningResourceService service;

    @GetMapping
    @Operation(summary = "List resources", description = "Paginated catalogue with optional title query.")
    public Page<ResourceView> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ResourceType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(query, type, page, size);
    }

    @GetMapping("/{id}")
    public ResourceView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResourceView create(@Valid @RequestBody UpsertResourceRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public ResourceView update(@PathVariable UUID id, @Valid @RequestBody UpsertResourceRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
