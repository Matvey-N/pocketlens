package com.pocketlens.importing.service;

import java.util.List;

/**
 * First rows of a file, used by the column-mapping screen (US-1.4).
 *
 * @param matchingFormats ids of built-in formats whose required columns are all present
 */
public record CsvPreview(String delimiter, List<String> headers, List<List<String>> rows,
                         List<String> matchingFormats) {
}
