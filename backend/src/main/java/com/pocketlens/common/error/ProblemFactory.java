package com.pocketlens.common.error;

import com.pocketlens.common.web.CorrelationIdFilter;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;

/**
 * Builds RFC 9457 problem details with the PocketLens extension members
 * ({@code code}, {@code correlationId}, {@code timestamp}).
 */
public final class ProblemFactory {

    private static final String TYPE_BASE = "https://pocketlens.app/problems/";

    private ProblemFactory() {
    }

    public static ProblemDetail create(ErrorCode code, String detail) {
        return create(code, detail, Map.of());
    }

    public static ProblemDetail create(ErrorCode code, String detail, Map<String, Object> extra) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setTitle(code.title());
        problem.setType(URI.create(TYPE_BASE + code.name().toLowerCase().replace('_', '-')));
        problem.setProperty("code", code.name());
        problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        problem.setProperty("timestamp", Instant.now().toString());
        extra.forEach(problem::setProperty);
        return problem;
    }
}
