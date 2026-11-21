package com.pocketlens.transaction.api.dto;

import com.pocketlens.transaction.service.CategoryUpdateResult;

public record UpdateTransactionResponse(TransactionResponse transaction, boolean ruleCreated, int recategorizedCount) {

    public static UpdateTransactionResponse from(CategoryUpdateResult result) {
        return new UpdateTransactionResponse(TransactionResponse.from(result.transaction()), result.ruleCreated(),
                result.recategorizedCount());
    }
}
