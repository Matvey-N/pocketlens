package com.pocketlens.importing.service;

import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ConflictException;
import com.pocketlens.common.error.ErrorCode;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.importing.domain.ImportProfile;
import com.pocketlens.importing.persistence.ImportProfileRepository;
import com.pocketlens.importing.service.format.ColumnMapping;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saved column mappings for banks without a built-in format (US-1.4).
 */
@Service
public class ImportProfileService {

    private final ImportProfileRepository profiles;
    private final Clock clock;

    public ImportProfileService(ImportProfileRepository profiles, Clock clock) {
        this.profiles = profiles;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ImportProfile> list(UUID userId) {
        return profiles.findByUserIdOrderByName(userId);
    }

    @Transactional
    public ImportProfile create(UUID userId, String name, ColumnMapping mapping) {
        validate(mapping);
        if (profiles.existsByUserIdAndNameIgnoreCase(userId, name.strip())) {
            throw new ConflictException("A mapping named '" + name.strip() + "' already exists");
        }
        return profiles.save(new ImportProfile(userId, name.strip(), mapping, clock.instant()));
    }

    @Transactional
    public void delete(UUID userId, UUID profileId) {
        ImportProfile profile = profiles.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new NotFoundException("Import profile", profileId));
        profiles.delete(profile);
    }

    void validate(ColumnMapping mapping) {
        boolean hasAmount = mapping.amount() != null && !mapping.amount().isBlank();
        boolean hasSplit = mapping.debit() != null && !mapping.debit().isBlank()
                && mapping.credit() != null && !mapping.credit().isBlank();
        if (hasAmount == hasSplit) {
            throw invalid("Map either an amount column or both debit and credit columns");
        }
        boolean hasCurrencyColumn = mapping.currency() != null && !mapping.currency().isBlank();
        boolean hasDefaultCurrency = mapping.defaultCurrency() != null && mapping.defaultCurrency().matches("[A-Z]{3}");
        if (!hasCurrencyColumn && !hasDefaultCurrency) {
            throw invalid("Map a currency column or choose a default currency");
        }
        try {
            DateTimeFormatter.ofPattern(mapping.dateFormat());
        } catch (IllegalArgumentException e) {
            throw invalid("Date format '" + mapping.dateFormat() + "' is not valid");
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, message);
    }
}
