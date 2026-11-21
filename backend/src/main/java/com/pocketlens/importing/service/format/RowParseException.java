package com.pocketlens.importing.service.format;

/**
 * A single CSV row contains an invalid value.
 */
public class RowParseException extends Exception {

    public RowParseException(String message) {
        super(message);
    }
}
