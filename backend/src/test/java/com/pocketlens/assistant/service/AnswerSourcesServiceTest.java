package com.pocketlens.assistant.service;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pocketlens.assistant.domain.Message;
import com.pocketlens.support.TestData;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.service.TransactionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnswerSourcesServiceTest {

    private final ConversationService conversations = mock(ConversationService.class);
    private final TransactionService transactions = mock(TransactionService.class);
    private final AnswerSourcesService service = new AnswerSourcesService(conversations, transactions);

    @Test
    void sumOfListedTransactionsEqualsTheFigureInTheAnswer() {
        Transaction a = TestData.transaction("2026-10-03", "-10.50", "LIDL");
        Transaction b = TestData.transaction("2026-10-20", "-4.50", "LIDL");
        Transaction c = TestData.transaction(USER_ID, "2026-10-21", "-7.00", "USD", "AMAZON");
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = Message.assistant(conversationId, "You spent 15.00 EUR", List.of(), List.of(),
                List.of(a.getId(), b.getId(), c.getId()), TestData.NOW);
        when(conversations.message(USER_ID, conversationId, messageId)).thenReturn(message);
        when(transactions.findByIds(USER_ID, message.getSourceTransactionIds())).thenReturn(List.of(a, b, c));

        AnswerSources sources = service.load(USER_ID, conversationId, messageId);

        assertThat(sources.transactions()).extracting(Transaction::getId).containsExactly(c.getId(), b.getId(), a.getId());
        assertThat(sources.totals()).hasSize(2);
        assertThat(sources.totals().get(0).currency()).isEqualTo("EUR");
        assertThat(sources.totals().get(0).amount()).isEqualByComparingTo("15.00");
        assertThat(sources.totals().get(1).count()).isEqualTo(1);
    }
}
