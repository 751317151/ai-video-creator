package com.avc.web.controller;

import com.avc.ai.planner.WeeklyPlannerService;
import com.avc.common.dto.request.WeeklyPlanRequest;
import com.avc.common.dto.response.ApiResponse;
import com.avc.common.dto.response.WeeklyPlanResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final WeeklyPlannerService weeklyPlannerService;

    @PostMapping("/weekly")
    public ApiResponse<WeeklyPlanResponse> generateWeeklyPlan(@Valid @RequestBody WeeklyPlanRequest request) {
        WeeklyPlanResponse plan = weeklyPlannerService.generatePlan(request);
        return ApiResponse.ok(plan);
    }
}
