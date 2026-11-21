package com.pocketlens.assistant.api.dto;

import com.pocketlens.assistant.service.AnswerSources;
import com.pocketlens.transaction.api.dto.TransactionResponse;
import java.util.List;

/**
 * The transactions behind an answer and their sums per currency (US-4.3: the sum equals the figure in the answer).
 */
public record SourceTransactionsResponse(List<TransactionResponse> transactions, List<AnswerSources.Total> totals) {

    public static SourceTransactionsResponse from(AnswerSources sources) {
        return new SourceTransactionsResponse(sources.transactions().stream().map(TransactionResponse::from).toList(),
                sources.totals());
    }
}
