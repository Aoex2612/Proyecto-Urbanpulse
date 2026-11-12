package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la representacion de un activo urbano asociado en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentAssetResponseV2 {

    private String assetId;
    private String assetCode;
    private String type;
    private String name;
    private Double latitude;
    private Double longitude;
    private Double distanceMeters;
    private Double confidenceScore;
    private Boolean isConfirmed;
}
