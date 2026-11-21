package com.pocketlens.importing.service;

import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ErrorCode;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.stereotype.Component;

/**
 * Decodes uploaded bytes (UTF-8 with or without BOM, fallback Windows-1252) and opens a CSV parser.
 */
@Component
public class CsvReader {

    private static final char BOM = '﻿';
    private static final Charset FALLBACK = Charset.forName("windows-1252");

    public String decode(byte[] content) {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException e) {
            text = new String(content, FALLBACK);
        }
        return !text.isEmpty() && text.charAt(0) == BOM ? text.substring(1) : text;
    }

    /** Guesses the delimiter from the header line. */
    public char detectDelimiter(String text) {
        String header = text.lines().findFirst().orElse("");
        Map<Character, Long> counts = Map.of(
                ',', header.chars().filter(c -> c == ',').count(),
                ';', header.chars().filter(c -> c == ';').count(),
                '\t', header.chars().filter(c -> c == '\t').count());
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse(',');
    }

    public CSVParser open(String text, char delimiter) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .setAllowMissingColumnNames(true)
                .setIgnoreHeaderCase(true)
                .build();
        try {
            return format.parse(new StringReader(text));
        } catch (IOException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.IMPORT_FILE_INVALID, "The file is not a valid CSV file: " + e.getMessage());
        }
    }

    public List<String> headers(CSVParser parser) {
        return parser.getHeaderNames().stream().map(String::strip).toList();
    }
}
