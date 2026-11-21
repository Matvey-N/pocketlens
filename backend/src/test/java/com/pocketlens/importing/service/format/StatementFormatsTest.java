package com.pocketlens.importing.service.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

class StatementFormatsTest {

    @Test
    void revolutImportsCompletedRowsAndAddsTheFee() throws Exception {
        RevolutCsvFormat format = new RevolutCsvFormat();
        List<CSVRecord> rows = parse(',', """
                Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
                CARD_PAYMENT,Current,2026-11-01 09:00:00,2026-11-02 10:15:00,Lidl,-12.30,0.20,EUR,COMPLETED,100
                CARD_PAYMENT,Current,2026-11-01 09:00:00,,Steam,-5.00,0.00,EUR,DECLINED,100
                """);

        Optional<ParsedTransaction> first = format.parse(rows.get(0));
        assertThat(first).get().satisfies(t -> {
            assertThat(t.date()).isEqualTo(LocalDate.of(2026, 11, 2));
            assertThat(t.amount()).isEqualByComparingTo("-12.50");
            assertThat(t.currency()).isEqualTo("EUR");
            assertThat(t.description()).isEqualTo("Lidl");
        });
        assertThat(format.parse(rows.get(1))).isEmpty();
        assertThat(format.id()).isEqualTo("revolut");
        assertThat(format.requiredColumns()).contains("State", "Fee");
    }

    @Test
    void europeanFormatCombinesDebitAndCredit() throws Exception {
        SemicolonBankCsvFormat format = new SemicolonBankCsvFormat();
        List<CSVRecord> rows = parse(';', """
                Date;Details;Debit;Credit;Currency
                03.11.2026;Maxima;1 234,50;;EUR
                04.11.2026;Salary;;2 000,00;EUR
                05.11.2026;Zero;;;EUR
                """);

        assertThat(format.parse(rows.get(0))).get().extracting(ParsedTransaction::amount)
                .isEqualTo(new BigDecimal("-1234.50"));
        assertThat(format.parse(rows.get(1))).get().extracting(ParsedTransaction::amount)
                .isEqualTo(new BigDecimal("2000.00"));
        assertThat(format.parse(rows.get(2))).isEmpty();
        assertThat(format.requiredColumns()).containsExactly("Date", "Details", "Debit", "Credit", "Currency");
        assertThat(format.delimiter()).isEqualTo(';');
    }

    @Test
    void mappedFormatUsesDefaultCurrencyAndValidatesValues() throws Exception {
        MappedStatementFormat format = new MappedStatementFormat("p", "My bank",
                new ColumnMapping(',', "When", "dd/MM/yyyy", "What", "Sum", null, null, null, "USD", '.'));
        List<CSVRecord> rows = parse(',', """
                When,What,Sum
                01/11/2026,Coffee,-3.20
                02/11/2026,,-1.00
                bad,Tea,-1.00
                """);

        assertThat(format.parse(rows.get(0))).get().extracting(ParsedTransaction::currency).isEqualTo("USD");
        assertThatThrownBy(() -> format.parse(rows.get(1))).hasMessageContaining("description");
        assertThatThrownBy(() -> format.parse(rows.get(2))).hasMessageContaining("date");
        assertThat(format.requiredColumns()).containsExactly("When", "What", "Sum");
        assertThat(format.displayName()).isEqualTo("My bank");
    }

    @Test
    void longDescriptionsAreTruncated() throws Exception {
        GenericCsvFormat format = new GenericCsvFormat();
        List<CSVRecord> rows = parse(',', "Date,Description,Amount,Currency\n2026-11-01," + "x".repeat(300)
                + ",-1.00,EUR\n");
        assertThat(format.parse(rows.get(0))).get().extracting(t -> t.description().length()).isEqualTo(255);
        assertThat(new WiseCsvFormat().id()).isEqualTo("wise");
    }

    private static List<CSVRecord> parse(char delimiter, String text) throws IOException {
        return CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true).setTrim(true)
                .build().parse(new StringReader(text)).getRecords();
    }
}
