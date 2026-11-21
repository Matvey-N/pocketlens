package com.pocketlens.assistant.service.tool;

/**
 * The model passed invalid arguments; the message is returned to the model so it can correct the call.
 */
public class ToolInputException extends Exception {

    public ToolInputException(String message) {
        super(message);
    }
}
