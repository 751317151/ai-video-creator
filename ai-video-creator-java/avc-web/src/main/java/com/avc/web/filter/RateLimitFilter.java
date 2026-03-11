package com.avc.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Order(2)
public class RateLimitFilter implements Filter {

    private final StringRedisTemplate redisTemplate;

    @Value("${avc.rate-limit.max-requests:60}")
    private int maxRequests;

    @Value("${avc.rate-limit.window-seconds:60}")
    private int windowSeconds;

    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of("""
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = redis.call('INCR', key)
            if current == 1 then
                redis.call('EXPIRE', key, window)
            end
            return current
            """, Long.class);

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String path = httpReq.getRequestURI();

        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpReq);
        String key = "rate_limit:" + clientIp;

        try {
            Long count = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(key),
                    String.valueOf(maxRequests),
                    String.valueOf(windowSeconds));

            if (count != null && count <= maxRequests) {
                chain.doFilter(request, response);
            } else {
                HttpServletResponse httpResp = (HttpServletResponse) response;
                httpResp.setStatus(429);
                httpResp.setContentType("application/json");
                httpResp.setHeader("Retry-After", String.valueOf(windowSeconds));
                httpResp.getWriter().write(
                        "{\"success\":false,\"error\":\"Rate limit exceeded\"}");
            }
        } catch (Exception e) {
            chain.doFilter(request, response);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
