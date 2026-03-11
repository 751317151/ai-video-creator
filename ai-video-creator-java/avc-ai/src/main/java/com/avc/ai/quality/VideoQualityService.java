package com.avc.ai.quality;

import com.avc.common.dto.request.QualityEvaluateRequest;
import com.avc.common.dto.response.QualityReport;
import com.avc.infra.mapper.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoQualityService {

    private final ChatClient chatClient;
    private final JobMapper jobMapper;

    public QualityReport evaluate(QualityEvaluateRequest request) {
        String prompt = String.format("""
                Evaluate video quality based on the following metadata:
                Title: %s
                Description: %s
                Duration: %d seconds
                Video path: %s

                Score each aspect from 1-10:
                - composition: Visual composition quality
                - relevance: Content relevance to topic
                - subtitleReadability: Subtitle clarity
                - overallAppeal: Overall viewer appeal

                Respond in JSON:
                {
                  "scores": {"composition": N, "relevance": N, "subtitleReadability": N, "overallAppeal": N},
                  "suggestion": "improvement suggestions",
                  "readyToPublish": true/false
                }
                """, request.title(), request.description(),
                request.durationSeconds(), request.videoPath());

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseQualityReport(response);
        } catch (Exception e) {
            log.error("Quality evaluation failed", e);
            return new QualityReport(
                    Map.of("composition", 0, "relevance", 0, "subtitleReadability", 0, "overallAppeal", 0),
                    "Evaluation failed: " + e.getMessage(), false);
        }
    }

    public QualityReport getReport(String jobId) {
        return Optional.ofNullable(jobMapper.selectById(jobId))
                .map(job -> evaluate(new QualityEvaluateRequest(
                        job.getVideoPath(), job.getTitle(),
                        job.getDescription(), job.getDurationSeconds())))
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }

    private QualityReport parseQualityReport(String response) {
        try {
            String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = om.readTree(cleaned);

            var scoresNode = node.path("scores");
            Map<String, Integer> scores = Map.of(
                    "composition", scoresNode.path("composition").asInt(5),
                    "relevance", scoresNode.path("relevance").asInt(5),
                    "subtitleReadability", scoresNode.path("subtitleReadability").asInt(5),
                    "overallAppeal", scoresNode.path("overallAppeal").asInt(5)
            );
            String suggestion = node.path("suggestion").asText("");
            boolean readyToPublish = node.path("readyToPublish").asBoolean(false);
            return new QualityReport(scores, suggestion, readyToPublish);
        } catch (Exception e) {
            log.warn("Failed to parse quality report", e);
            return new QualityReport(Map.of(), "Parse error", false);
        }
    }
}
