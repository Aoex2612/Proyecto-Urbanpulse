package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO para la representacion de un comentario en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseV2 {

    private String id;
    private String incidentId;
    private String authorId;
    private String authorName;
    private String comment;
    private Boolean isInternal;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
