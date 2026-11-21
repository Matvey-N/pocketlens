package com.pocketlens.ai.llm;

import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ErrorCode;

/**
 * The LLM provider is not configured, timed out or returned an error (UC-1 A2, UC-2 A2).
 */
public class AiUnavailableException extends ApiException {

    public AiUnavailableException(String message) {
        super(ErrorCode.AI_UNAVAILABLE, message);
    }
}
