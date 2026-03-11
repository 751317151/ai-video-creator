package com.avc.app.filter;

import com.avc.web.filter.RateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private FilterChain chain;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(redisTemplate);
        ReflectionTestUtils.setField(filter, "maxRequests", 60);
        ReflectionTestUtils.setField(filter, "windowSeconds", 60);
    }

    @Test
    void shouldPassWhenUnderLimit() throws IOException, ServletException {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(1L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/videos");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldReturn429WhenOverLimit() throws IOException, ServletException {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(61L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/videos");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Rate limit exceeded");
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        verifyNoInteractions(chain);
    }

    @Test
    void shouldSkipNonApiPaths() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldAllowRequestOnRedisFailure() throws IOException, ServletException {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RuntimeException("Redis connection failed"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/videos");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldUseXForwardedForWhenPresent() throws IOException, ServletException {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("rate_limit:10.0.0.1")), any()))
                .thenReturn(1L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/videos");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldPassAtExactLimit() throws IOException, ServletException {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(60L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/videos");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }
}
