package com.pocketlens.user.api;

import com.pocketlens.common.security.CurrentUserId;
import com.pocketlens.user.api.dto.DeleteAccountRequest;
import com.pocketlens.user.api.dto.UserResponse;
import com.pocketlens.user.service.UserDataExport;
import com.pocketlens.user.service.UserService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The current user's account (US-6.3, US-6.4).
 */
@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse me(@CurrentUserId UUID userId) {
        return UserResponse.from(userService.get(userId));
    }

    @GetMapping("/export")
    public ResponseEntity<UserDataExport> export(@CurrentUserId UUID userId) {
        UserDataExport export = userService.export(userId);
        String fileName = "pocketlens-export-" + LocalDate.ofInstant(export.exportedAt(), ZoneOffset.UTC) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build()
                        .toString())
                .body(export);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@CurrentUserId UUID userId, @Valid @RequestBody DeleteAccountRequest request) {
        userService.delete(userId, request.password());
        return ResponseEntity.noContent().build();
    }
}
