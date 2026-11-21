package com.pocketlens.importing.service;

/**
 * Uploaded file content, independent of the web layer.
 */
public record ImportFile(String fileName, String contentType, byte[] content) {
}
