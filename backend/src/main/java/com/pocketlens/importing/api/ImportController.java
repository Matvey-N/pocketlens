package com.pocketlens.importing.api;

import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ErrorCode;
import com.pocketlens.common.security.CurrentUserId;
import com.pocketlens.importing.api.dto.FormatResponse;
import com.pocketlens.importing.api.dto.ImportBatchResponse;
import com.pocketlens.importing.service.CsvPreview;
import com.pocketlens.importing.service.ImportFile;
import com.pocketlens.importing.service.ImportService;
import com.pocketlens.importing.service.ImportSummary;
import com.pocketlens.importing.service.StatementFormatRegistry;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Statement import (UC-1, US-1.1, US-1.2, US-1.4).
 */
@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {

    private final ImportService importService;
    private final StatementFormatRegistry registry;

    public ImportController(ImportService importService, StatementFormatRegistry registry) {
        this.importService = importService;
        this.registry = registry;
    }

    @GetMapping("/formats")
    public List<FormatResponse> formats() {
        return registry.all().stream().map(FormatResponse::from).toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportSummary> importStatement(@CurrentUserId UUID userId,
                                                         @RequestPart("file") MultipartFile file,
                                                         @RequestParam(required = false) String format,
                                                         @RequestParam(required = false) UUID profileId) {
        ImportSummary summary = importService.importStatement(userId, toImportFile(file), format, profileId);
        return ResponseEntity.created(URI.create("/api/v1/imports/" + summary.batchId())).body(summary);
    }

    @PostMapping(path = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvPreview preview(@RequestPart("file") MultipartFile file) {
        return importService.preview(toImportFile(file));
    }

    @GetMapping
    public List<ImportBatchResponse> history(@CurrentUserId UUID userId) {
        return importService.history(userId).stream().map(ImportBatchResponse::from).toList();
    }

    private static ImportFile toImportFile(MultipartFile file) {
        try {
            return new ImportFile(file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new ApiException(ErrorCode.IMPORT_FILE_INVALID, "The uploaded file could not be read");
        }
    }
}
