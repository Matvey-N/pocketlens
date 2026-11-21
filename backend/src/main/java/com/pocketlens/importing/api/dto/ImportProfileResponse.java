package com.pocketlens.importing.api.dto;

import com.pocketlens.importing.domain.ImportProfile;
import com.pocketlens.importing.service.format.ColumnMapping;
import java.time.Instant;
import java.util.UUID;

public record ImportProfileResponse(UUID id, String name, String delimiter, String dateColumn, String dateFormat,
                                    String descriptionColumn, String amountColumn, String debitColumn,
                                    String creditColumn, String currencyColumn, String defaultCurrency,
                                    String decimalSeparator, Instant createdAt) {

    public static ImportProfileResponse from(ImportProfile profile) {
        ColumnMapping m = profile.toMapping();
        return new ImportProfileResponse(profile.getId(), profile.getName(), String.valueOf(m.delimiter()), m.date(),
                m.dateFormat(), m.description(), m.amount(), m.debit(), m.credit(), m.currency(), m.defaultCurrency(),
                String.valueOf(m.decimalSeparator()), profile.getCreatedAt());
    }
}
