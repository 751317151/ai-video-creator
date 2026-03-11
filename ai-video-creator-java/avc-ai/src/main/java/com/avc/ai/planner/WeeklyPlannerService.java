package com.avc.ai.planner;

import com.avc.ai.rag.ScriptKnowledgeService;
import com.avc.common.dto.request.WeeklyPlanRequest;
import com.avc.common.dto.response.WeeklyPlanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyPlannerService {

    private final ChatClient chatClient;
    private final ScriptKnowledgeService scriptKnowledgeService;

    public WeeklyPlanResponse generatePlan(WeeklyPlanRequest request) {
        List<String> relatedScripts = scriptKnowledgeService.search(
                request.niche() + " " + request.targetAudience(), 5);

        String context = relatedScripts.isEmpty() ? "No historical data available." :
                "Successful past scripts:\n" + String.join("\n---\n", relatedScripts);

        String prompt = String.format("""
                Create a weekly video content plan:
                Niche: %s
                Target audience: %s
                Videos per day: %d

                Historical context:
                %s

                For each day (Monday-Sunday), provide:
                - Topic
                - Video type (knowledge/news/story/tutorial)
                - Best publish time
                - Brief description

                Respond in JSON format:
                {
                  "days": [
                    {
                      "day": "Monday",
                      "videos": [
                        {"topic": "...", "videoType": "...", "publishTime": "HH:mm", "description": "..."}
                      ]
                    }
                  ],
                  "summary": "Overall strategy summary"
                }
                """, request.niche(), request.targetAudience(), request.videosPerDay());

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseWeeklyPlan(response);
        } catch (Exception e) {
            log.error("Failed to generate weekly plan", e);
            return new WeeklyPlanResponse(List.of(), "Failed to generate plan: " + e.getMessage());
        }
    }

    private WeeklyPlanResponse parseWeeklyPlan(String response) {
        try {
            String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = om.readTree(cleaned);

            List<WeeklyPlanResponse.DayPlan> days = new ArrayList<>();
            node.path("days").forEach(dayNode -> {
                String day = dayNode.path("day").asText();
                List<WeeklyPlanResponse.VideoSlot> videos = new ArrayList<>();
                dayNode.path("videos").forEach(v -> videos.add(new WeeklyPlanResponse.VideoSlot(
                        v.path("topic").asText(),
                        v.path("videoType").asText(),
                        v.path("publishTime").asText(),
                        v.path("description").asText()
                )));
                days.add(new WeeklyPlanResponse.DayPlan(day, videos));
            });
            String summary = node.path("summary").asText("");
            return new WeeklyPlanResponse(days, summary);
        } catch (Exception e) {
            log.warn("Failed to parse weekly plan", e);
            return new WeeklyPlanResponse(List.of(), "Parse error: " + e.getMessage());
        }
    }
}
