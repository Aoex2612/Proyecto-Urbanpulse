# Integración de NotificationService en el Ciclo de Vida de Incidencias

## Resumen del Componente
Este módulo orquesta la generación y emisión automática de notificaciones persistentes in-app (`Notification`) ante cualquier transición de estado del ciclo de vida operativo de una incidencia urbana en UrbanPulse (RF-7, RF-8 de la HU21).

## Destinatarios y Reglas de Negocio
- **Ciudadano Creador:** Recibe siempre una notificación de tipo `STATUS_CHANGE` informándole de la evolución de su reporte (`incident.getCitizen()`).
- **Técnicos Asignados Activos:** Se consulta `TareasRepository.findByIncidentIdAndIsActiveTrue(incidentId)` para notificar a cada técnico con asignación activa en el expediente.
- **Plantilla de Mensaje:**
  - **Título:** `Incidencia #{id} actualizada a {nuevoEstado}`
  - **Mensaje:** `La incidencia '{titulo}' ha cambiado de estado de {estadoAnterior} a {nuevoEstado}.`
  - **Tipo:** `STATUS_CHANGE`

## Métodos Modificados
1. `IncidentTriageService`:
   - `validateIncident`: Transición `REPORTED` &rarr; `VALIDATED`.
   - `rejectIncident`: Transición a `REJECTED`.
   - `reevaluateIncident`: Transición de `REJECTED` a `REPORTED` o `VALIDATED`.
   - `startIntervention`: Transición a `IN_PROGRESS`.
   - `resolveIncident`: Transición a `RESOLVED`.
   - `closeIncident`: Transición a `CLOSED`.
   - `reopenIncident`: Transición a `REOPENED`.
   - Método auxiliar privado `sendStatusNotifications(Incidencia, String, String)`.

2. `IncidentService`:
   - `updateIncidentStatus`: Transiciones de estado generales con auditoría.
   - Método auxiliar privado `sendStatusNotificationsFromService(Incidencia, String, String)`.

## Pruebas Automatizadas
- `IncidentTriageServiceTest`: `shouldSendNotificationOnValidateIncident` valida la emisión de eventos tanto al ciudadano emisor como al personal técnico asignado.
- `IncidentServiceTest`: `shouldSendNotificationOnUpdateIncidentStatus` verifica la invocación a `NotificationService.createNotification` en la transición atómica.
