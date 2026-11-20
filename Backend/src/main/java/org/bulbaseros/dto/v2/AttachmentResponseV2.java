package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO para la representacion de un archivo adjunto en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponseV2 {

    private String id;
    private String incidentId;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private Long fileSizeBytes;
    private OffsetDateTime uploadedAt;
}
