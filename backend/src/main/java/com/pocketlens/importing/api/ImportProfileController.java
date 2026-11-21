package com.pocketlens.importing.api;

import com.pocketlens.common.security.CurrentUserId;
import com.pocketlens.importing.api.dto.ImportProfileRequest;
import com.pocketlens.importing.api.dto.ImportProfileResponse;
import com.pocketlens.importing.domain.ImportProfile;
import com.pocketlens.importing.service.ImportProfileService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Saved column mappings (US-1.4).
 */
@RestController
@RequestMapping("/api/v1/import-profiles")
public class ImportProfileController {

    private final ImportProfileService profileService;

    public ImportProfileController(ImportProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public List<ImportProfileResponse> list(@CurrentUserId UUID userId) {
        return profileService.list(userId).stream().map(ImportProfileResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<ImportProfileResponse> create(@CurrentUserId UUID userId,
                                                        @Valid @RequestBody ImportProfileRequest request) {
        ImportProfile profile = profileService.create(userId, request.name(), request.toMapping());
        return ResponseEntity.created(URI.create("/api/v1/import-profiles/" + profile.getId()))
                .body(ImportProfileResponse.from(profile));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        profileService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
