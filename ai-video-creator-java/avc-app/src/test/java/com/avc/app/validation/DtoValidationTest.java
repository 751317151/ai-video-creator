package com.avc.app.validation;

import com.avc.common.dto.request.ChatRequest;
import com.avc.common.dto.request.ModerationCheckRequest;
import com.avc.common.dto.request.VideoCreateRequest;
import com.avc.common.dto.request.WeeklyPlanRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void videoCreateRequest_shouldRejectBlankTopic() {
        VideoCreateRequest request = new VideoCreateRequest(
                "", null, null, null, null,
                null, null, false, false, null, null, 0.3, null
        );
        Set<ConstraintViolation<VideoCreateRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("topic"));
    }

    @Test
    void videoCreateRequest_shouldRejectOversizedTopic() {
        String longTopic = "a".repeat(501);
        VideoCreateRequest request = new VideoCreateRequest(
                longTopic, null, null, null, null,
                null, null, false, false, null, null, 0.3, null
        );
        Set<ConstraintViolation<VideoCreateRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("topic"));
    }

    @Test
    void videoCreateRequest_shouldAcceptValidRequest() {
        VideoCreateRequest request = new VideoCreateRequest(
                "How to cook pasta", null, null, null, null,
                "zh-CN-XiaoxiaoNeural", null, false, false, null, null, 0.3, null
        );
        Set<ConstraintViolation<VideoCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void chatRequest_shouldRejectBlankMessage() {
        ChatRequest request = new ChatRequest("session-1", "");
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("message"));
    }

    @Test
    void chatRequest_shouldRejectOversizedMessage() {
        ChatRequest request = new ChatRequest("session-1", "a".repeat(5001));
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("message"));
    }

    @Test
    void chatRequest_shouldAcceptValid() {
        ChatRequest request = new ChatRequest("session-1", "Create a video about AI");
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void moderationCheckRequest_shouldRejectBlankContent() {
        ModerationCheckRequest request = new ModerationCheckRequest("", null);
        Set<ConstraintViolation<ModerationCheckRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
    }

    @Test
    void moderationCheckRequest_shouldAcceptValid() {
        ModerationCheckRequest request = new ModerationCheckRequest("Check this content", "script");
        Set<ConstraintViolation<ModerationCheckRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void weeklyPlanRequest_shouldRejectBlankNiche() {
        WeeklyPlanRequest request = new WeeklyPlanRequest("", "general audience", 2);
        Set<ConstraintViolation<WeeklyPlanRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("niche"));
    }

    @Test
    void weeklyPlanRequest_shouldAcceptValid() {
        WeeklyPlanRequest request = new WeeklyPlanRequest("tech reviews", "developers", 3);
        Set<ConstraintViolation<WeeklyPlanRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}
