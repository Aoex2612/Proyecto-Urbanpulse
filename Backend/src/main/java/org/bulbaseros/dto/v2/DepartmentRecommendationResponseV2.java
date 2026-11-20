package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la recomendacion asistida de departamento en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRecommendationResponseV2 {

    private String suggestedDepartmentId;
    private String suggestedDepartmentName;
    private Double confidencePercentage;
    private Boolean isLowConfidenceWarning;
    private String rationale;
}
