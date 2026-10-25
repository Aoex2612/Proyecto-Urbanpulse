# 003 — Mapa de Incidencias con Leaflet y Auditoría Transversal — Plan

_Cómo se implementa lo descrito en `spec.md`. Respeta estrictamente la `constitution/` y las convenciones de UrbanPulse._

## Enfoque

La solución aborda de forma unificada la dimensión geoespacial y la trazabilidad del ciclo de vida de las incidencias mediante dos ejes complementarios:
1. **Cartografía interactiva con Leaflet en Frontend:** Incorporación de una vista `/map` protegida que consume teselas de OpenStreetMap sin costes ni claves propietarias. Ofrece una experiencia reactiva que permite explorar incidentes urbanos ya reportados y, mediante un flujo guiado ("Crear incidencia"), capturar las coordenadas exactas con un clic para dar de alta nuevos avisos en la API REST.
2. **Auditoría inmutable transversal en Backend:** Aprovechamiento del esquema relacional ya existente en PostgreSQL (`status_changes` e `incidents`), integrando la persistencia transaccional de cada cambio de estado en `CambiosEstado` con captura automática de la identidad del usuario desde el token JWT (`SecurityContextHolder`). Exposición de un endpoint seguro `GET /api/incidents/{id}/history` restringido a `ADMIN`, consumido por un componente desacoplado `Timeline` diseñado para permitir su rápida reutilización por otros perfiles operativos (`OPERATOR`).

---

## Implementación

_Pasos técnicos ordenados por capas y módulos afectados:_

### 1. Backend: Capa de Persistencia y Repositorios
- **Verificación y ajuste de Entidades:**
  - `Backend/src/main/java/org/bulbaseros/entity/Incidencia.java`: Asegurar mapeo completo de campos (`id`, `title`, `description`, `category`, `latitude`, `longitude`, `status`, `priority`, `citizen`, `createdAt`, `updatedAt`).
  - `Backend/src/main/java/org/bulbaseros/entity/CambiosEstado.java`: Verificar relaciones `@ManyToOne` con `Incidencia` y `Usuario`, y atributos `fromStatus`, `toStatus`, `reason`, `createdAt`.
- **Creación de Repositorios JPA:**
  - `Backend/src/main/java/org/bulbaseros/repository/IncidenciaRepository.java`: Consultas de lectura general y persistencia de incidencias.
  - `Backend/src/main/java/org/bulbaseros/repository/CambiosEstadoRepository.java`: Método `findByIncidentIdOrderByCreatedAtAsc(Long incidentId)` para recuperar el historial ordenado de transiciones.

### 2. Backend: DTOs y Mapeo de Datos
- **Creación de DTOs para Incidencias y Auditoría:**
  - `Backend/src/main/java/org/bulbaseros/dto/CreateIncidentRequest.java`: Validaciones Jakarta con `@NotBlank` para título, descripción y categoría, y `@NotNull` para latitud y longitud.
  - `Backend/src/main/java/org/bulbaseros/dto/IncidentResponse.java`: Representación pública de la incidencia para marcadores del mapa y vistas de detalle.
  - `Backend/src/main/java/org/bulbaseros/dto/StatusChangeResponse.java`: Representación de la transición histórica (`id`, `incidentId`, `fromStatus`, `toStatus`, `changedByUserId`, `changedByUserName`, `changedByUserEmail`, `reason`, `createdAt`).

### 3. Backend: Capa de Servicio y Reglas de Negocio
- **Creación de `IncidentService` (`Backend/src/main/java/org/bulbaseros/service/IncidentService.java`):**
  - `List<IncidentResponse> getAllIncidents()`: Retorna todas las incidencias registradas.
  - `IncidentResponse getIncidentById(Long id)`: Obtiene una incidencia por su identificador o lanza excepción 404.
  - `IncidentResponse createIncident(CreateIncidentRequest request, String userEmail)`: Busca al usuario emisor por email, inicializa la entidad con estado `REPORTED`, prioridad `MEDIUM` y fecha actual, y persiste en base de datos.
  - `IncidentStatusResponse updateIncidentStatus(Long id, UpdateIncidentStatusRequest request, String userEmail)`: En una única transacción (`@Transactional`), actualiza el campo `status` de la incidencia, compone y almacena el registro en `CambiosEstado` registrando el estado anterior, el nuevo y el motivo, y retorna la respuesta.
  - `List<StatusChangeResponse> getIncidentHistory(Long id)`: Valida la existencia de la incidencia y retorna su historial cronológico ordenado.

### 4. Backend: Controladores y Seguridad Perimetral
- **Evolución de `IncidentController` (`Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`):**
  - `GET /api/incidents`: Retorna la lista de incidencias para pintar el mapa (autorizado para cualquier usuario autenticado).
  - `GET /api/incidents/{id}`: Retorna el detalle de una incidencia concreta (autenticado).
  - `POST /api/incidents`: Alta de incidencia extrayendo la identidad de `Authentication` (HTTP 201).
  - `PATCH /api/incidents/{id}/status`: Actualización con auditoría transaccional (`OPERATOR`, `ADMIN`).
  - `GET /api/incidents/{id}/history`: Consulta de auditoría (`ADMIN`).
- **Ajuste de Reglas en `SecurityConfig` (`Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java`):**
  - Declarar explícitamente:
    - `.requestMatchers(HttpMethod.GET, "/api/incidents/{id}/history").hasRole("ADMIN")` (extensible en el futuro a `"OPERATOR"` con `hasAnyRole("ADMIN", "OPERATOR")`).
    - `.requestMatchers(HttpMethod.PATCH, "/api/incidents/*/status").hasAnyRole("OPERATOR", "ADMIN")`.
    - `.requestMatchers("/api/incidents/**").authenticated()`.

### 5. Frontend: Dependencias y Servicio API
- **Instalación de paquetes cartográficos:**
  - `leaflet` y `react-leaflet` en `frontend/package.json`.
  - Importación global de la hoja de estilos `leaflet/dist/leaflet.css`.
- **Capa de servicio (`frontend/src/services/incidentService.js`):**
  - Métodos `getAllIncidents()`, `createIncident(data)`, `updateStatus(id, data)` y `getIncidentHistory(id)`.

### 6. Frontend: Componentes del Mapa y Modal de Alta
- **Componente Cartográfico Principal (`frontend/src/components/Map/IncidentsMap.jsx`):**
  - Integración de `MapContainer`, `TileLayer` y `Marker` con teselas de OpenStreetMap, fijando el centro inicial en Málaga (`lat: 36.7213`, `lng: -4.4214`, zoom 13).
  - Listener para eventos de clic en el mapa (`useMapEvents`) cuando el modo de creación esté activo, capturando latitud y longitud y levantando el modal de alta.
  - Corrección de rutas de iconos de marcadores de Leaflet en Vite/React.
- **Modal de Creación Asistida (`frontend/src/components/Map/CreateIncidentModal.jsx`):**
  - Formulario con campos: Título, Descripción, Categoría y coordenadas fijadas en modo solo lectura.
  - Validación de campos y feedback de éxito/error.

### 7. Frontend: Detalle de Incidencia y Componente Timeline
- **Componente de Línea Temporal (`frontend/src/components/Timeline/StatusTimeline.jsx`):**
  - Componente desacoplado que renderiza los hitos cronológicos con fecha formateada, autor, estado origen/destino y justificación.
  - Diseñado con props genéricas (`history`, `loading`, `error`) para facilitar su incrustación tanto en modales de mapa como en paneles operativos.
- **Modal de Detalle (`frontend/src/components/Map/IncidentDetailModal.jsx`):**
  - Visualización de datos básicos de la incidencia para cualquier usuario autenticado.
  - Verificación del rol del usuario (`user.role === 'ADMIN'`) para invocar el servicio de historial y mostrar `StatusTimeline`.
  - Preparado para que si se amplía a `OPERATOR` baste con actualizar la condición a `['ADMIN', 'OPERATOR'].includes(user.role)`.

### 8. Frontend: Vistas y Enrutamiento
- **Página de Mapa (`frontend/src/pages/MapPage.jsx` y `MapPage.css`):**
  - Barra de herramientas con botón "Crear incidencia" / "Cancelar selección", selector de vista e integración del mapa a pantalla completa.
- **Enrutamiento en `App.jsx` y Barra de Navegación:**
  - Registro de la ruta protegida `<Route path="/map" element={<ProtectedRoute><MapPage /></ProtectedRoute>} />`.
  - Enlace de acceso directo "Mapa" en la cabecera/navegación de `HomePage` y demás vistas cuando el usuario ha iniciado sesión.

---

## Decisiones

- **Uso de Leaflet y OpenStreetMap en lugar de APIs comerciales (Google Maps / Mapbox):**
  - _Justificación:_ OpenStreetMap + Leaflet son de código abierto, no requieren registro de claves API ni facturación por uso, y se integran limpiamente en entornos locales y educativos sin barreras perimetrales.
- **Persistencia síncrona transaccional en `status_changes`:**
  - _Justificación:_ Toda modificación de estado debe garantizar consistencia atómica entre el nuevo estado de la incidencia y la fila de auditoría. Si falla la inserción en el historial, la actualización del estado debe revertirse (`@Transactional`).
- **Extracción de identidad mediante el JWT (`SecurityContextHolder`) y no por parámetro de petición:**
  - _Justificación:_ Evita la suplantación de identidad en la auditoría; el actor registrado en `changed_by_user_id` es siempre el titular autenticado del token que emitió la llamada HTTP.
- **Diseño desacoplado del componente `Timeline`:**
  - _Justificación:_ Cumple con la exigencia expresa de permitir que la visualización del historial sea extensible a otros roles (como `OPERATOR`) con solo cambiar una regla de autorización, sin rehacer componentes visuales.
- **Reutilización del esquema DDL existente en PostgreSQL:**
  - _Justificación:_ El script `database/init/diagramUrbanPulse.sql` y las entidades de dominio ya prevén las tablas `incidents` y `status_changes`. No es necesario alterar la base de datos, sino conectar la capa de servicios y repositorios.

---

## Riesgos

- **Incompatibilidad de iconos en Leaflet con empaquetadores modernos (Vite):**
  - _Mitigación:_ Configurar explícitamente en el inicio del componente los iconos por defecto (`L.Icon.Default.mergeOptions`) usando imports directos o URLs absolutas de iconos de Leaflet para evitar marcadores rotos ("missing marker image").
- **Concurrencia en cambios de estado simultáneos sobre la misma incidencia:**
  - _Mitigación:_ Control transaccional en Spring Data JPA garantizando lecturas consistentes de `from_status` y `to_status`.
- **Rendimiento ante un volumen elevado de incidencias en el mapa:**
  - _Mitigación:_ Consulta de campos esenciales en `IncidentResponse` y posicionamiento de marcadores estándar de Leaflet sin sobrecargar el DOM.

---

## Estrategia de tests

- **Backend (Unitarios con Mockito y Spring Boot Test):**
  - `IncidentServiceTest`:
    - Creación de incidencia asignando usuario emisor y estado inicial `REPORTED`.
    - Actualización de estado generando correctamente el registro en `CambiosEstado` con usuario autenticado y estados correspondientes.
    - Rechazo de actualización con 404 ante incidencia inexistente.
    - Consulta de historial retornando lista cronológica.
  - `IncidentControllerSecurityTest`:
    - Petición anónima a `GET /api/incidents` responde 401 Unauthorized.
    - Petición con rol `CITIZEN` a `PATCH /api/incidents/{id}/status` responde 403 Forbidden.
    - Petición con rol `OPERATOR` a `PATCH /api/incidents/{id}/status` responde 200 OK.
    - Petición con rol `OPERATOR` o `CITIZEN` a `GET /api/incidents/{id}/history` responde 403 Forbidden.
    - Petición con rol `ADMIN` a `GET /api/incidents/{id}/history` responde 200 OK con el listado de cambios.
- **Frontend (Linting y Verificación de Flujo):**
  - Verificación con ESLint (`npm run lint`).
  - Comprobación manual del renderizado del mapa, alta de marcador tras creación y visibilidad condicional de `Timeline` según rol del usuario en sesión.
