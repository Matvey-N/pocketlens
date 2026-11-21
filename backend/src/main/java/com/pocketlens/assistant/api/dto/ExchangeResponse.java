package com.pocketlens.assistant.api.dto;

import com.pocketlens.assistant.service.AssistantAnswer;

public record ExchangeResponse(MessageResponse question, MessageResponse answer) {

    public static ExchangeResponse from(AssistantAnswer answer) {
        return new ExchangeResponse(MessageResponse.from(answer.question()), MessageResponse.from(answer.answer()));
    }
}
