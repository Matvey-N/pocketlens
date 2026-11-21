package com.pocketlens.dashboard.api;

import com.pocketlens.common.security.CurrentUserId;
import com.pocketlens.dashboard.service.DashboardService;
import com.pocketlens.dashboard.service.DashboardView;
import java.time.Clock;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final Clock clock;

    public DashboardController(DashboardService dashboardService, Clock clock) {
        this.dashboardService = dashboardService;
        this.clock = clock;
    }

    @GetMapping
    public DashboardView get(@CurrentUserId UUID userId,
                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return dashboardService.build(userId, month == null ? YearMonth.now(clock) : month);
    }
}
