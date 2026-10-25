# 003 — Mapa de Incidencias con Leaflet y Auditoría Transversal — Plan

_Cómo se implementa lo descrito en `spec.md`. Respeta estrictamente la `constitution/` y las convenciones de UrbanPulse._

## Enfoque

La solución aborda de forma unificada la dimensión geoespacial y la trazabilidad del ciclo de vida de las incidencias mediante dos ejes complementarios:
1. **Cartografía interactiva con Leaflet en Frontend:** Incorporación de una vista `/map` protegida centrada en la ciudad de Málaga (`[36.7213, -4.4214]`, zoom 13) que consume teselas de OpenStreetMap. Muestra marcadores para las incidencias no terminadas (`status NOT IN ('CLOSED', 'REJECTED')`). Al hacer clic en un marcador, despliega un Popup nativo de Leaflet con la información básica y un botón de acción "Ver detalle completo" que levanta un modal de detalle (con la línea temporal `Timeline` para usuarios con rol `ADMIN`). Ofrece un flujo guiado ("Crear incidencia" / "Cancelar selección") para capturar coordenadas geográficas precisas al hacer clic en el mapa y abrir el formulario modal de alta con categorías predefinidas (`Alumbrado`, `Vías Públicas`, `Limpieza`, `Parques y Jardines`, `Mobiliario Urbano`), descartando el punto limpiamente si el usuario cancela.
2. **Auditoría inmutable transversal en Backend:** Aprovechamiento del esquema relacional ya existente en PostgreSQL (`status_changes` e `incidents`), integrando la persistencia transaccional de cada cambio de estado en `CambiosEstado` con captura automática de la identidad del usuario desde el token JWT (`SecurityContextHolder`). Exposición de un endpoint seguro `GET /api/incidents/{id}/history` restringido a `ADMIN` (con manejo estricto de 404 si la incidencia no existe), consumido por un componente desacoplado `Timeline` diseñado para permitir su rápida reutilización por otros perfiles operativos (`OPERATOR`).

---

## Implementación

_Pasos técnicos ordenados por capas y módulos afectados:_

### 1. Backend: Capa de Persistencia y Repositorios
- **Entidades de Dominio:**
  - `Backend/src/main/java/org/bulbaseros/entity/Incidencia.java`: Mapeo completo con tabla `incidents` (`id`, `title`, `description`, `category`, `latitude`, `longitude`, `status`, `priority`, `citizen`, `createdAt`, `updatedAt`).
  - `Backend/src/main/java/org/bulbaseros/entity/CambiosEstado.java`: Mapeo completo con tabla `status_changes` (`id`, `incident`, `fromStatus`, `toStatus`, `changedByUser`, `reason`, `createdAt`).
- **Repositorios Spring Data JPA:**
  - `Backend/src/main/java/org/bulbaseros/repository/IncidenciaRepository.java`: Consultas de lectura filtrando estados no terminados (`findByStatusNotIn(List<String> statuses)` o query JPQL) y persistencia de nuevas incidencias.
  - `Backend/src/main/java/org/bulbaseros/repository/CambiosEstadoRepository.java`: Método `findByIncidentIdOrderByCreatedAtAsc(Long incidentId)` para recuperar el historial cronológico de transiciones.

### 2. Backend: DTOs y Mapeo de Datos
- **DTOs de Petición y Respuesta:**
  - `Backend/src/main/java/org/bulbaseros/dto/CreateIncidentRequest.java`: Validaciones Jakarta con `@NotBlank` para título, descripción y categoría (validada contra catálogo o texto normalizado), y `@NotNull` para latitud y longitud.
  - `Backend/src/main/java/org/bulbaseros/dto/IncidentResponse.java`: Representación pública de la incidencia para marcadores del mapa y modal de detalle (`id`, `title`, `description`, `category`, `latitude`, `longitude`, `status`, `priority`, `citizenId`, `citizenName`, `createdAt`).
  - `Backend/src/main/java/org/bulbaseros/dto/StatusChangeResponse.java`: Representación de la transición histórica (`id`, `incidentId`, `fromStatus`, `toStatus`, `changedByUserId`, `changedByUserName`, `changedByUserEmail`, `reason`, `createdAt`).

### 3. Backend: Capa de Servicio y Reglas de Negocio
- **Lógica en `IncidentService` (`Backend/src/main/java/org/bulbaseros/service/IncidentService.java`):**
  - `List<IncidentResponse> getActiveIncidents()`: Retorna todas las incidencias cuyo estado no sea `CLOSED` ni `REJECTED`.
  - `IncidentResponse getIncidentById(Long id)`: Obtiene una incidencia por su identificador o lanza `ResourceNotFoundException` (HTTP 404).
  - `IncidentResponse createIncident(CreateIncidentRequest request, String userEmail)`: Busca al usuario emisor por email, inicializa la entidad con estado `REPORTED`, prioridad `MEDIUM` y marca temporal actual, y persiste en base de datos.
  - `IncidentStatusResponse updateIncidentStatus(Long id, UpdateIncidentStatusRequest request, String userEmail)`: En una única transacción (`@Transactional`), valida existencia de la incidencia, actualiza el campo `status`, compone y almacena el registro en `CambiosEstado` registrando el estado anterior, el nuevo y el motivo, y retorna la respuesta.
  - `List<StatusChangeResponse> getIncidentHistory(Long id)`: Valida la existencia de la incidencia (lanzando 404 si no existe) y retorna su historial cronológico ordenado.

### 4. Backend: Controladores y Seguridad Perimetral
- **Controlador REST `IncidentController` (`Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`):**
  - `GET /api/incidents`: Retorna la lista de incidencias activas para el mapa (autorizado para usuarios autenticados de cualquier rol).
  - `GET /api/incidents/{id}`: Retorna el detalle de una incidencia concreta (autenticado).
  - `POST /api/incidents`: Alta de incidencia extrayendo la identidad de `Authentication` (HTTP 201).
  - `PATCH /api/incidents/{id}/status`: Actualización con auditoría transaccional (`OPERATOR`, `ADMIN`).
  - `GET /api/incidents/{id}/history`: Consulta de auditoría (`ADMIN`).
- **Seguridad en `SecurityConfig` (`Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java`):**
  - Declarar explícitamente:
    - `.requestMatchers(HttpMethod.GET, "/api/incidents/{id}/history").hasRole("ADMIN")`.
    - `.requestMatchers(HttpMethod.PATCH, "/api/incidents/*/status").hasAnyRole("OPERATOR", "ADMIN")`.
    - `.requestMatchers("/api/incidents/**").authenticated()`.

### 5. Frontend: Dependencias y Servicio API
- **Instalación de paquetes cartográficos:**
  - `leaflet` y `react-leaflet` en `frontend/package.json`.
  - Importación global de la hoja de estilos `leaflet/dist/leaflet.css`.
- **Capa de servicio (`frontend/src/services/incidentService.js`):**
  - Métodos `getAllIncidents()`, `getIncidentById(id)`, `createIncident(data)`, `updateStatus(id, data)` y `getIncidentHistory(id)`.

### 6. Frontend: Componentes del Mapa y Modal de Alta
- **Componente Cartográfico Principal (`frontend/src/components/Map/IncidentsMap.jsx`):**
  - Integración de `MapContainer`, `TileLayer` y `Marker` con teselas de OpenStreetMap, fijando el centro inicial en Málaga (`lat: 36.7213`, `lng: -4.4214`, zoom 13).
  - Cada marcador renderiza un Popup nativo de Leaflet con título, descripción, categoría, estado y botón "Ver detalle completo".
  - Listener para eventos de clic en el mapa (`useMapEvents`) activo únicamente cuando el modo de creación esté habilitado; si no está activo, no genera acciones y solo cierra popups abiertos.
  - Corrección de rutas de iconos de marcadores de Leaflet en Vite/React.
- **Modal de Creación Asistida (`frontend/src/components/Map/CreateIncidentModal.jsx`):**
  - Formulario con campos: Título, Descripción, selector desplegable de Categoría (`Alumbrado`, `Vías Públicas`, `Limpieza`, `Parques y Jardines`, `Mobiliario Urbano`) y coordenadas prefijadas en modo solo lectura.
  - Botón cancelar que descarta las coordenadas capturadas y cierra el modal sin dejar marcadores huérfanos.

### 7. Frontend: Detalle de Incidencia y Componente Timeline
- **Componente de Línea Temporal (`frontend/src/components/Timeline/StatusTimeline.jsx`):**
  - Componente desacoplado que renderiza los hitos cronológicos con fecha formateada, autor, estado origen/destino y justificación.
  - Diseñado con props genéricas (`history`, `loading`, `error`) para facilitar su incrustación en modales o futuros paneles operativos.
- **Modal de Detalle (`frontend/src/components/Map/IncidentDetailModal.jsx`):**
  - Despliega la información completa de la incidencia tras pulsar "Ver detalle completo" en el Popup del mapa.
  - Evalúa el rol del usuario en sesión (`user.role === 'ADMIN'`); si es administrador, invoca `incidentService.getIncidentHistory(id)` y renderiza `StatusTimeline`.
  - Preparado para extender la visibilidad a `OPERATOR` actualizando una simple condición de autorización.

### 8. Frontend: Vistas y Enrutamiento
- **Página de Mapa (`frontend/src/pages/MapPage.jsx` y `MapPage.css`):**
  - Barra superior con botón toggle "Crear incidencia" / "Cancelar selección", selector de vista e integración del mapa a pantalla completa.
- **Enrutamiento en `App.jsx` y Navegación:**
  - Registro de la ruta protegida `<Route path="/map" element={<ProtectedRoute><MapPage /></ProtectedRoute>} />`.
  - Enlace de acceso directo "Mapa" en la cabecera/navegación accesible para usuarios autenticados.

---

## Decisiones

- **Filtrado de incidencias terminadas (`CLOSED`, `REJECTED`) en la vista del mapa:**
  - _Justificación:_ Evita la saturación visual del mapa con avisos ya concluidos o descartados, priorizando los problemas urbanos pendientes de atención.
- **Popup nativo sobre marcador combinado con modal de detalle:**
  - _Justificación:_ Permite una consulta ágil y ligera de los datos básicos directamente en el mapa sin perder el contexto territorial, reservando el modal para la información detallada y la línea temporal de auditoría.
- **Catálogo cerrado de categorías en desplegable:**
  - _Justificación:_ Estandariza la clasificación de problemas urbanos facilitando futuros algoritmos de triaje y enrutamiento interdepartamental.
- **Botón toggle 'Crear incidencia' / 'Cancelar selección':**
  - _Justificación:_ Proporciona una interfaz intuitiva y controlada que evita la creación accidental de incidencias por clics involuntarios en el mapa.
- **Desacoplamiento del componente `Timeline`:**
  - _Justificación:_ Garantiza que habilitar la visualización del historial al rol `OPERATOR` requiera únicamente modificar una regla de acceso sin refactorizar la lógica visual.

---

## Riesgos

- **Incompatibilidad de iconos en Leaflet con empaquetadores modernos (Vite):**
  - _Mitigación:_ Configurar explícitamente en el inicio del componente los iconos por defecto (`L.Icon.Default.mergeOptions`) usando imports directos o URLs absolutas de iconos de Leaflet para evitar marcadores rotos ("missing marker image").
- **Concurrencia en cambios de estado simultáneos sobre la misma incidencia:**
  - _Mitigación:_ Control transaccional en Spring Data JPA garantizando lecturas y escrituras atómicas de `from_status` y `to_status`.
- **Rendimiento cartográfico ante incremento de incidencias:**
  - _Mitigación:_ Filtrado de estados terminados en backend y consumo ligero de marcadores en React.

---

## Estrategia de tests

- **Backend (Unitarios con Mockito y Spring Boot Test):**
  - `IncidentServiceTest`:
    - Filtrado de incidencias activas excluyendo `CLOSED` y `REJECTED`.
    - Creación de incidencia asignando usuario emisor, categoría válida y estado inicial `REPORTED`.
    - Actualización de estado generando correctamente el registro en `CambiosEstado`.
    - Rechazo con 404 ante actualización o consulta de historial sobre incidencia inexistente.
    - Consulta de historial retornando lista cronológica para `ADMIN`.
  - `IncidentControllerSecurityTest`:
    - Petición anónima a `GET /api/incidents` responde 401 Unauthorized.
    - Petición con rol `CITIZEN`, `OPERATOR`, `TECHNICIAN` o `ANALYST` a `GET /api/incidents/{id}/history` responde 403 Forbidden.
    - Petición con rol `ADMIN` a `GET /api/incidents/{id}/history` responde 200 OK (o 404 si la incidencia no existe).
- **Frontend (Linting y Verificación de Flujo):**
  - Verificación con ESLint (`npm run lint`).
  - Comprobación manual de interacción en el mapa centrado en Málaga, apertura de Popup, despliegue del modal de detalle con `Timeline` condicional y cancelación limpia de creación.
