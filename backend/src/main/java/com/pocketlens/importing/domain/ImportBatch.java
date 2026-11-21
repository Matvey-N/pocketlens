package com.pocketlens.importing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * History entry of one imported file with its summary (UC-1, step 7).
 */
@Entity
@Table(name = "import_batches")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "format_name", nullable = false, length = 80)
    private String formatName;

    @Column(name = "rows_read", nullable = false)
    private int rowsRead;

    @Column(name = "rows_imported", nullable = false)
    private int rowsImported;

    @Column(name = "rows_duplicate", nullable = false)
    private int rowsDuplicate;

    @Column(name = "rows_skipped", nullable = false)
    private int rowsSkipped;

    @Column(name = "rows_needs_review", nullable = false)
    private int rowsNeedsReview;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ImportBatch() {
    }

    public ImportBatch(UUID userId, String fileName, String formatName, Instant createdAt) {
        this.userId = userId;
        this.fileName = fileName;
        this.formatName = formatName;
        this.createdAt = createdAt;
    }

    public void complete(int read, int imported, int duplicates, int skipped, int needsReview) {
        this.rowsRead = read;
        this.rowsImported = imported;
        this.rowsDuplicate = duplicates;
        this.rowsSkipped = skipped;
        this.rowsNeedsReview = needsReview;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFormatName() {
        return formatName;
    }

    public int getRowsRead() {
        return rowsRead;
    }

    public int getRowsImported() {
        return rowsImported;
    }

    public int getRowsDuplicate() {
        return rowsDuplicate;
    }

    public int getRowsSkipped() {
        return rowsSkipped;
    }

    public int getRowsNeedsReview() {
        return rowsNeedsReview;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
