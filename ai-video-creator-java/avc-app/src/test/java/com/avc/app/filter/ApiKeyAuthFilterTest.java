package com.avc.app.filter;

import com.avc.web.filter.ApiKeyAuthFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ApiKeyAuthFilterTest {

    private ApiKeyAuthFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter();
        chain = mock(FilterChain.class);
    }

    @Test
    void shouldReturn500WhenApiKeyNotConfigured() throws IOException, ServletException {
        ReflectionTestUtils.setField(filter, "apiKey", "");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/videos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("API key not set");
        verifyNoInteractions(chain);
    }

    @Test
    void shouldReturn401WhenApiKeyMissing() throws IOException, ServletException {
        ReflectionTestUtils.setField(filter, "apiKey", "correct-key");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/videos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or missing API key");
        verifyNoInteractions(chain);
    }

    @Test
    void shouldReturn401WhenApiKeyWrong() throws IOException, ServletException {
        ReflectionTestUtils.setField(filter, "apiKey", "correct-key");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/videos");
        request.addHeader("X-API-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void shouldPassWithCorrectApiKey() throws IOException, ServletException {
        ReflectionTestUtils.setField(filter, "apiKey", "correct-key");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/videos");
        request.addHeader("X-API-Key", "correct-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldSkipSwaggerPaths() throws IOException, ServletException {
        ReflectionTestUtils.setField(filter, "apiKey", "");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldSkipActuatorHealthPath() throws IOException, ServletException {
        ReflectionTestUtils.setField(filter, "apiKey", "");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldSkipRootPath() throws IOException, ServletException {
        ReflectionTestUtils.setField(filter, "apiKey", "");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldSkipWebSocketPath() throws IOException, ServletException {
        ReflectionTestUtils.setField(filter, "apiKey", "");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/info");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
