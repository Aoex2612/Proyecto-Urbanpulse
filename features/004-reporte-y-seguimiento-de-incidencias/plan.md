# 004 — Reporte y Seguimiento de Incidencias con Detección Territorial — Plan

_Cómo se implementa lo descrito en `spec.md`. Respeta estrictamente la `constitution/`, los principios de diseño de UrbanPulse y el [ADR-06](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/docs/ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md)._

## Enfoque

La solución amplía la experiencia ciudadana integrando contexto geográfico oficial y trazabilidad personal del ciclo de vida a través de tres componentes técnicos:
1. **Resolución territorial asistida, resiliente y desacoplada (Backend):** Creación del servicio `MalagaOpenDataService` en el backend para consultar el callejero y delimitaciones territoriales. Se implementa con un cliente HTTP configurado con una factoría SSL permisiva (`SimpleClientHttpRequestFactory` con `SSLContext` y `HostnameVerifier`) para admitir certificados de administraciones públicas sin provocar cortes por `SunCertPathBuilderException`. Se establece un timeout estricto de 3 segundos y una arquitectura de contingencia: si el portal municipal falla, responde vacío o da error, se activa un fallback automático a la API de geocodificación inversa de OpenStreetMap / Nominatim (`https://nominatim.openstreetmap.org/reverse`). Si ambas fuentes se agotan, se devuelven valores nulos (`null`) sin bloquear la creación de la incidencia (degradación controlada). Se expone el endpoint `GET /api/incidents/geocode/reverse` para el frontend.
2. **Alta robusta con límites municipales (Backend & Frontend):** Validación del término municipal de Málaga mediante el bounding box oficial (latitud `[36.5000, 36.9000]`, longitud `[-4.7000, -4.2000]`). En el formulario modal de alta (`CreateIncidentModal`), los campos de dirección, barrio y distrito se presentan en modo de solo lectura; si alguno es nulo, se muestra la etiqueta informativa `"Sin asignar"`.
3. **Seguimiento personal paginado "Mis Incidencias" (Backend & Frontend):** Implementación del endpoint `GET /api/incidents/my-incidents` con paginación (`Pageable`, 10 elementos por defecto) y filtro opcional por estado (`status`), extrayendo de forma segura la identidad del emisor desde el token JWT. En el frontend, se crea la página protegida `/my-incidents` (`MyIncidentsPage`) con listado cronológico descendente, selector de estado (`TODOS` + estados oficiales), visualización de fechas clave (`createdAt`, `updatedAt`) y acceso al modal interactivo de detalle de la incidencia (`IncidentDetailModal`).

---

## Implementación

_Pasos técnicos ordenados por capas y módulos afectados:_

### 1. Backend: DTOs, Repositorio y Mapeadores
- **DTOs de Transferencia:**
  - `Backend/src/main/java/org/bulbaseros/dto/ReverseGeocodeResponse.java`: DTO para la respuesta de geocodificación inversa conteniendo `address`, `district` y `neighborhood`.
  - `Backend/src/main/java/org/bulbaseros/dto/PageResponse.java`: DTO genérico o estructurado para envolver respuestas paginadas (`content`, `totalElements`, `totalPages`, `page`, `size`) conforme a `docs/api.md`.
- **Extensión de Repositorio JPA (`Backend/src/main/java/org/bulbaseros/repository/IncidenciaRepository.java`):**
  - Añadir métodos paginados:
    - `Page<Incidencia> findByCitizenId(Long citizenId, Pageable pageable);`
    - `Page<Incidencia> findByCitizenIdAndStatus(Long citizenId, EstadoIncidencia status, Pageable pageable);`
- **Mapeador (`Backend/src/main/java/org/bulbaseros/mappers/IncidentMapper.java`):**
  - Incorporar método de conveniencia para convertir páginas `Page<Incidencia>` a `PageResponse<IncidentResponse>`.

### 2. Backend: Integración con Datos Abiertos de Málaga y Contingencia Nominatim
- **Servicio `MalagaOpenDataService` (`Backend/src/main/java/org/bulbaseros/service/MalagaOpenDataService.java`):**
  - Configuración de cliente HTTP (`RestTemplate`) con `ConnectTimeout` y `ReadTimeout` de 3000 ms y factoría segura `createSecureClientHttpRequestFactory()`.
  - Configuración de cabecera `User-Agent: UrbanPulse-App/1.0`.
  - Inyección de propiedad de fallback `nominatim.base-url` desde `application.properties`.
  - Método `ReverseGeocodeResponse resolveLocation(Double latitude, Double longitude)`:
    - Valida que las coordenadas pertenezcan al término municipal de Málaga.
    - Intenta resolver con el portal de datos abiertos municipal.
    - Ante excepción de red, timeout, error HTTP o respuesta vacía, conmuta limpiamente a `resolveWithNominatim(...)`.
    - Si todas las fuentes fallan, retorna campos nulos bajo degradación controlada.
  - Parser extensible `parseTerritorialResponse(String json)` que soporta esquemas Nominatim (`address.road`, `address.suburb`, `address.city_district`), CKAN (`result.records`), GeoJSON y planos.

### 3. Backend: Lógica de Negocio y Controladores
- **Actualización de `IncidentService` (`Backend/src/main/java/org/bulbaseros/service/IncidentService.java`):**
  - Inyección de `MalagaOpenDataService`.
  - En `createIncident(CreateIncidentRequest request, String userEmail)`:
    - Validar rango geográfico de Málaga (`[36.5000, 36.9000]`, `[-4.7000, -4.2000]`).
    - Si `district` o `neighborhood` no vienen prefijados, invocar `malagaOpenDataService.resolveLocation(...)` para autocompletar campos antes de persistir con estado `REPORTED` y prioridad `MEDIUM`.
  - Método `PageResponse<IncidentResponse> getMyIncidents(String userEmail, EstadoIncidencia status, Pageable pageable)`:
    - Recupera incidencias del usuario autenticado ordenadas descendentemente por `createdAt`.
  - Método `ReverseGeocodeResponse reverseGeocode(Double latitude, Double longitude)`:
    - Expone la resolución territorial validando coordenadas.
- **Controlador REST `IncidentController` (`Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`):**
  - `GET /api/incidents/geocode/reverse?latitude={lat}&longitude={lng}`: Endpoint protegido para previsualizar dirección y territorio.
  - `GET /api/incidents/my-incidents`: Endpoint protegido para listado paginado del ciudadano autenticado.
  - `GET /api/incidents/{id}`: Endpoint existente con validación 404 ante IDs inexistentes.

### 4. Frontend: Capa de Servicios y API
- **`frontend/src/services/incidentService.js`:**
  - `getMyIncidents(params)`: Realiza petición `GET /api/incidents/my-incidents` con query params (`page`, `size`, `status`).
  - `reverseGeocode(latitude, longitude)`: Invoca `GET /api/incidents/geocode/reverse` para resolver la dirección al colocar el marcador.

### 5. Frontend: Formulario de Creación con Solo Lectura
- **`frontend/src/components/Map/CreateIncidentModal.jsx`:**
  - Al recibir coordenadas del mapa, solicita geocodificación inversa a `incidentService.reverseGeocode(...)`.
  - Renderiza campos bloqueados de solo lectura para Dirección, Barrio y Distrito.
  - Muestra `"Sin asignar"` si los valores son nulos.

### 6. Frontend: Vista "Mis Incidencias" y Navegación
- **Página `frontend/src/pages/MyIncidentsPage.jsx` y `MyIncidentsPage.css`:**
  - Listado paginado (10 avisos por página) con filtro por estado (`TODOS` + estados del ciclo de vida).
  - Botón de acción "Ver detalle" que despliega `IncidentDetailModal` con fechas clave y localización.
  - Estado vacío descriptivo con botón para reportar en el mapa `/map`.
- **Enrutamiento en `frontend/src/App.jsx`:**
  - Registrar ruta `<Route path="/my-incidents" element={<ProtectedRoute><MyIncidentsPage /></ProtectedRoute>} />`.
  - Enlace de navegación en barra superior para usuarios autenticados.

---

## Decisiones

- **Integración centralizada con Datos Abiertos y fallback en Backend (ver [ADR-06](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/docs/ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md)):**
  - _Justificación:_ Garantiza política estricta de degradación controlada (timeout 3s), resuelve problemas de confianza en certificados SSL de administraciones públicas y proporciona resiliencia inmediata mediante Nominatim sin exponer clientes frontend a bloqueos de red o CORS.
- **Factoría SSL permisiva (`createSecureClientHttpRequestFactory`):**
  - _Justificación:_ Evita excepciones `SunCertPathBuilderException` producidas cuando el servidor remoto usa certificados de CAs españolas (FNMT, Camerfirma) no incluidas en el truststore estándar de OpenJDK.
- **Campos territoriales estrictamente de solo lectura:**
  - _Justificación:_ La posición geográfica marcada en el mapa es la fuente de verdad espacial; previene inconsistencias humanas.
- **Paginación nativa y modal para visualización de detalle:**
  - _Justificación:_ Eficiencia óptima de consulta y experiencia interactiva en cliente sin necesidad de recargar rutas para cada ficha de detalle.

---

## Riesgos

- **Fallo de validación de certificados SSL de organismos públicos:**
  - _Mitigación:_ Factoría HTTP con `SSLContext` permisivo y `HostnameVerifier` en `MalagaOpenDataService`.
- **Latencia excesiva o caída del portal municipal de Málaga:**
  - _Mitigación:_ Conmutación automática a Nominatim OpenStreetMap; si ambas fallan o exceden 3s, degradación controlada persistiendo `null` y mostrando `"Sin asignar"`.
- **Coordenadas fuera del término municipal:**
  - _Mitigación:_ Validación geométrica de delimitación (bounding box) arrojando HTTP 400 Bad Request.

---

## Estrategia de tests

- **Backend:**
  - **Pruebas unitarias de `MalagaOpenDataService`:** Simulación de respuestas exitosas (CKAN, GeoJSON y Nominatim), fallback automático ante error 500 o timeout, factoría SSL y degradación controlada a `null`.
  - **Pruebas de persistencia (`@DataJpaTest`) en `IncidenciaRepository`:** Consultas paginadas por ciudadano y estado con ordenación descendente.
  - **Pruebas de servicio (`IncidentServiceTest`):** Autocompletado territorial, degradación a null, validación de coordenadas y aislamiento por ciudadano.
  - **Pruebas de controlador (`IncidentControllerTest`):** Verificación de endpoints con JWT, paginación, validación de errores 400 y 404.
- **Frontend:**
  - Pruebas unitarias con Vitest de `CreateIncidentModal`, `MyIncidentsPage` y `incidentService`.
