package com.avc.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Component
@Order(1)
public class ApiKeyAuthFilter implements Filter {

    @Value("${avc.api-key:}")
    private String apiKey;

    private static final Set<String> SKIP_PREFIXES = Set.of(
            "/swagger", "/v3/api-docs", "/actuator/health", "/actuator/info",
            "/ws", "/css", "/js", "/assets", "/index", "/favicon"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        String path = httpReq.getRequestURI();

        if (path.equals("/") || SKIP_PREFIXES.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        if (apiKey == null || apiKey.isBlank()) {
            HttpServletResponse httpResp = (HttpServletResponse) response;
            httpResp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            httpResp.setContentType("application/json");
            httpResp.getWriter().write(
                    "{\"success\":false,\"error\":\"Server misconfigured: API key not set\"}");
            return;
        }

        String requestKey = httpReq.getHeader("X-API-Key");
        if (requestKey != null && constantTimeEquals(apiKey, requestKey)) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse httpResp = (HttpServletResponse) response;
            httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResp.setContentType("application/json");
            httpResp.getWriter().write(
                    "{\"success\":false,\"error\":\"Invalid or missing API key\"}");
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
