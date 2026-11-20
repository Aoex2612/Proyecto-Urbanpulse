package org.bulbaseros.controller.v2;

import org.bulbaseros.dto.v2.NotificationResponseV2;
import org.bulbaseros.security.JwtService;
import org.bulbaseros.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerV2Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "operario@malaga.es")
    @DisplayName("GET /api/v2/notifications debe retornar notificaciones con id UUID")
    void getUserNotifications_shouldReturnNotifications() throws Exception {
        UUID notifId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        NotificationResponseV2 notif = NotificationResponseV2.builder()
                .id(notifId.toString())
                .userId(UUID.randomUUID().toString())
                .incidentId(incidentId.toString())
                .type("ASSIGNMENT")
                .title("Nueva asignacion")
                .message("Se le ha asignado una incidencia")
                .isRead(false)
                .createdAt(OffsetDateTime.now())
                .build();

        when(notificationService.getUserNotificationsV2("operario@malaga.es"))
                .thenReturn(List.of(notif));

        mockMvc.perform(get("/api/v2/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(notifId.toString()))
                .andExpect(jsonPath("$[0].incidentId").value(incidentId.toString()))
                .andExpect(jsonPath("$[0].isRead").value(false));
    }

    @Test
    @WithMockUser(username = "operario@malaga.es")
    @DisplayName("PATCH /api/v2/notifications/{id}/read debe marcar como leida usando UUID")
    void markAsRead_shouldUpdateStatus() throws Exception {
        UUID notifId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        NotificationResponseV2 readNotif = NotificationResponseV2.builder()
                .id(notifId.toString())
                .userId(UUID.randomUUID().toString())
                .incidentId(incidentId.toString())
                .type("ASSIGNMENT")
                .title("Nueva asignacion")
                .message("Se le ha asignado una incidencia")
                .isRead(true)
                .createdAt(OffsetDateTime.now())
                .build();

        when(notificationService.markAsReadV2(notifId, "operario@malaga.es"))
                .thenReturn(readNotif);

        mockMvc.perform(patch("/api/v2/notifications/" + notifId + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notifId.toString()))
                .andExpect(jsonPath("$.isRead").value(true));

        verify(notificationService).markAsReadV2(notifId, "operario@malaga.es");
    }
}
