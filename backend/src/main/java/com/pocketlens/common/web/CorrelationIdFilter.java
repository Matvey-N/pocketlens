package com.pocketlens.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a correlation id to every request (or accepts a valid one from the {@code X-Correlation-Id} header),
 * puts it into the logging context and the response header, and writes one structured access-log line.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String correlationId = incoming != null && VALID_ID.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();
        long started = System.nanoTime();
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            if (!request.getRequestURI().startsWith("/actuator")) {
                log.info("http_request method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            }
            MDC.remove(MDC_KEY);
        }
    }
}
