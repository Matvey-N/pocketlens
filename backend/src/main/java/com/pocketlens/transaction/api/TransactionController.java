package com.pocketlens.transaction.api;

import com.pocketlens.common.security.CurrentUserId;
import com.pocketlens.common.web.PageResponse;
import com.pocketlens.transaction.api.dto.TransactionResponse;
import com.pocketlens.transaction.api.dto.UpdateTransactionRequest;
import com.pocketlens.transaction.api.dto.UpdateTransactionResponse;
import com.pocketlens.transaction.service.TransactionFilter;
import com.pocketlens.transaction.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    /** Allowed sort keys mapped to entity properties (whitelist, no arbitrary property names). */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "date", "bookingDate", "amount", "amount", "merchant", "merchant");

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public PageResponse<TransactionResponse> list(
            @CurrentUserId UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean uncategorized,
            @RequestParam(required = false) Boolean needsReview,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) @Pattern(regexp = "^[A-Za-z]{3}$") String currency,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "date,desc")
            @Pattern(regexp = "^(date|amount|merchant),(asc|desc)$") String sort) {
        String[] parts = sort.split(",");
        Sort order = Sort.by(Sort.Direction.fromString(parts[1]), SORT_FIELDS.get(parts[0]))
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        TransactionFilter filter = new TransactionFilter(from, to, categoryId, uncategorized, needsReview, q, currency);
        return PageResponse.of(transactionService.search(userId, filter, PageRequest.of(page, size, order)),
                TransactionResponse::from);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return TransactionResponse.from(transactionService.get(userId, id));
    }

    @PatchMapping("/{id}")
    public UpdateTransactionResponse updateCategory(@CurrentUserId UUID userId, @PathVariable UUID id,
                                                    @Valid @RequestBody UpdateTransactionRequest request) {
        return UpdateTransactionResponse.from(
                transactionService.updateCategory(userId, id, request.categoryId(), request.applyToMerchant()));
    }
}
