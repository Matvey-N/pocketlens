package com.pocketlens.importing.api.dto;

import com.pocketlens.importing.service.format.ColumnMapping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Column mapping for a bank without a built-in format.
 */
public record ImportProfileRequest(
        @NotBlank @Size(max = 60) String name,
        @NotBlank @Pattern(regexp = "^[,;\\t|]$", message = "must be one of , ; | or tab") String delimiter,
        @NotBlank @Size(max = 80) String dateColumn,
        @NotBlank @Size(max = 40) String dateFormat,
        @NotBlank @Size(max = 80) String descriptionColumn,
        @Size(max = 80) String amountColumn,
        @Size(max = 80) String debitColumn,
        @Size(max = 80) String creditColumn,
        @Size(max = 80) String currencyColumn,
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO currency code") String defaultCurrency,
        @NotBlank @Pattern(regexp = "^[.,]$", message = "must be '.' or ','") String decimalSeparator) {

    public ColumnMapping toMapping() {
        return new ColumnMapping(delimiter.charAt(0), dateColumn.strip(), dateFormat.strip(),
                descriptionColumn.strip(), blankToNull(amountColumn), blankToNull(debitColumn),
                blankToNull(creditColumn), blankToNull(currencyColumn), blankToNull(defaultCurrency),
                decimalSeparator.charAt(0));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
