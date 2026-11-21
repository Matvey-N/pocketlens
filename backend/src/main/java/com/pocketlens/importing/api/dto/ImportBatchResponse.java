package com.pocketlens.importing.api.dto;

import com.pocketlens.importing.domain.ImportBatch;
import java.time.Instant;
import java.util.UUID;

public record ImportBatchResponse(UUID id, String fileName, String format, int rowsRead, int imported,
                                  int duplicates, int skipped, int needsReview, Instant createdAt) {

    public static ImportBatchResponse from(ImportBatch b) {
        return new ImportBatchResponse(b.getId(), b.getFileName(), b.getFormatName(), b.getRowsRead(),
                b.getRowsImported(), b.getRowsDuplicate(), b.getRowsSkipped(), b.getRowsNeedsReview(),
                b.getCreatedAt());
    }
}
