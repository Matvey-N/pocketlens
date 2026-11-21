package com.pocketlens.assistant.service;

import com.pocketlens.common.config.AppProperties;
import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ErrorCode;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates assistant questions: length limit and filter of known prompt-injection patterns (NFR-S4).
 * The filter is a first line of defence; the main protection is that tools only read the user's own data.
 */
@Component
public class PromptGuard {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore (all |any )?(the )?(previous|prior|above) (instructions|rules|prompts?)"),
            Pattern.compile("disregard (all |any )?(the )?(previous|prior|above|system)"),
            Pattern.compile("(reveal|show|print|repeat) (me )?(your|the) (system )?(prompt|instructions)"),
            Pattern.compile("you are now (?!able)"),
            Pattern.compile("developer mode|jailbreak|\\bdan mode\\b"),
            Pattern.compile("</?\\s*(system|assistant|tool)\\s*>"),
            Pattern.compile("(other|another|all) users?'? (data|transactions)"),
            Pattern.compile("игнорируй (все )?(предыдущие|прошлые) инструкции"));

    private final int maxLength;

    public PromptGuard(AppProperties properties) {
        this.maxLength = properties.assistant().maxQuestionLength();
    }

    /** Returns the trimmed question or throws {@code PROMPT_REJECTED}. */
    public String check(String question) {
        String value = question == null ? "" : question.strip();
        if (value.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "The question is empty");
        }
        if (value.length() > maxLength) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "The question is longer than " + maxLength + " characters");
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (INJECTION_PATTERNS.stream().anyMatch(p -> p.matcher(lower).find())) {
            throw new ApiException(ErrorCode.PROMPT_REJECTED,
                    "This question cannot be processed. Ask about your own spending, budgets or subscriptions.");
        }
        return value;
    }
}
