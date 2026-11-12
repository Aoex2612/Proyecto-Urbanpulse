package org.bulbaseros.controller.v2;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.bulbaseros.dto.AnalyticsFilterRequest;
import org.bulbaseros.dto.AnalyticsSummaryResponse;
import org.bulbaseros.dto.v2.DepartmentRecommendationResponseV2;
import org.bulbaseros.service.AnalyticsService;
import org.bulbaseros.service.IntelligentRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para analitica urbana y sugerencia asistida en la API v2.
 * Opera con identificadores UUID publicos.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@RestController
@RequestMapping("/api/v2/analytics")
@PreAuthorize("hasAnyRole('ANALYST', 'ADMIN', 'OPERATOR')")
@Slf4j
public class AnalyticsControllerV2 {

    private final AnalyticsService analyticsService;
    private final IntelligentRecommendationService intelligentRecommendationService;

    @Autowired
    public AnalyticsControllerV2(AnalyticsService analyticsService,
                                 IntelligentRecommendationService intelligentRecommendationService) {
        this.analyticsService = analyticsService;
        this.intelligentRecommendationService = intelligentRecommendationService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getAnalyticsSummary(
            @Valid @ModelAttribute AnalyticsFilterRequest filterRequest) {
        log.info("Petición REST GET /api/v2/analytics/summary con filtros: {}", filterRequest);
        return ResponseEntity.ok(analyticsService.getAnalyticsSummary(filterRequest));
    }

    @GetMapping("/recommend-department")
    public ResponseEntity<DepartmentRecommendationResponseV2> recommendDepartment(
            @RequestParam(required = false) UUID incidentId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category) {
        log.info("Petición REST GET /api/v2/analytics/recommend-department [incidentId={}, title={}, category={}]",
                incidentId, title, category);

        return ResponseEntity.ok(intelligentRecommendationService.recommendDepartmentV2(
                incidentId, title, description, category
        ));
    }
}
