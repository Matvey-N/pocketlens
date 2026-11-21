package com.pocketlens.assistant.service;

import com.pocketlens.assistant.domain.Message;

/**
 * The stored question and answer of one exchange.
 */
public record AssistantAnswer(Message question, Message answer) {
}
