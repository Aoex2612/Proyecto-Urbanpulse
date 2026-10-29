# 004 — Reporte y Seguimiento de Incidencias con Detección Territorial — Plan

_Cómo se implementa lo descrito en `spec.md`. Respeta estrictamente la `constitution/`, los principios de diseño de UrbanPulse y el [ADR-06](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/docs/ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md)._

## Enfoque

La solución amplía la experiencia ciudadana integrando contexto geográfico oficial y trazabilidad personal del ciclo de vida a través de tres componentes técnicos:
1. **Resolución territorial asistida y desacoplada (Backend):** Creación del servicio `MalagaOpenDataService` en el backend para consultar el callejero y delimitaciones de distritos y barrios desde la plataforma de Datos Abiertos del Ayuntamiento de Málaga (`datosabiertos.malaga.eu`). Se implementa con un cliente HTTP configurado con un timeout estricto de 3 segundos y degradación controlada: si la fuente externa no responde, falla o no encuentra coincidencia, se devuelven valores nulos (`null`) sin bloquear la creación de la incidencia. Se expone además un endpoint `GET /api/incidents/geocode/reverse` para permitir al frontend resolver y mostrar la dirección de solo lectura al marcar un punto en el mapa.
2. **Alta robusta con límites municipales (Backend & Frontend):** Validación del término municipal de Málaga mediante el bounding box oficial (latitud `[36.5000, 36.9000]`, longitud `[-4.7000, -4.2000]`). En el formulario modal de alta (`CreateIncidentModal`), los campos de dirección, barrio y distrito se presentan en modo de solo lectura; si alguno es nulo, se muestra la etiqueta informativa `"Sin asignar"`.
3. **Seguimiento personal paginado "Mis Incidencias" (Backend & Frontend):** Implementación del endpoint `GET /api/incidents/my-incidents` con paginación (`Pageable`, 10 elementos por defecto) y filtro opcional por estado (`status`), extrayendo de forma segura la identidad del emisor desde el token JWT. En el frontend, se crea la página protegida `/my-incidents` (`MyIncidentsPage`) con listado cronológico descendente, selector de estado (`TODOS` + estados oficiales), visualización de fechas clave (`createdAt`, `updatedAt`) y acceso al modal de detalle de la incidencia.

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

### 2. Backend: Integración con Datos Abiertos de Málaga
- **Servicio `MalagaOpenDataService` (`Backend/src/main/java/org/bulbaseros/service/MalagaOpenDataService.java`):**
  - Configuración de cliente HTTP (`RestClient` o `RestTemplate`) con `ConnectTimeout` y `ReadTimeout` de 3000 ms.
  - Método `ReverseGeocodeResponse resolveLocation(Double latitude, Double longitude)`:
    - Valida que las coordenadas estén dentro del término municipal de Málaga; si están fuera, lanza excepción de validación o rechazo.
    - Consulta la API/recursos del callejero y distritos del Ayuntamiento de Málaga.
    - En caso de error de red, respuesta inesperada o timeout, captura la excepción, registra log de advertencia y retorna un objeto con campos nulos para asegurar degradación controlada.

### 3. Backend: Lógica de Negocio y Controladores
- **Actualización de `IncidentService` (`Backend/src/main/java/org/bulbaseros/service/IncidentService.java`):**
  - Inyección de `MalagaOpenDataService`.
  - En `createIncident(CreateIncidentRequest request, String userEmail)`:
    - Validar rango geográfico de Málaga (`[36.5000, 36.9000]`, `[-4.7000, -4.2000]`).
    - Si `district` o `neighborhood` no vienen calculados, invocar `malagaOpenDataService.resolveLocation(...)` para poblar automáticamente los campos antes de persistir la entidad con estado `REPORTED` y prioridad `MEDIUM`.
  - Método `PageResponse<IncidentResponse> getMyIncidents(String userEmail, EstadoIncidencia status, Pageable pageable)`:
    - Busca al usuario por su email del JWT; si el parámetro `status` es nulo o `"TODOS"`, consulta `findByCitizenId`; de lo contrario, filtra por `findByCitizenIdAndStatus`.
    - Retorna la respuesta paginada ordenada descendentemente por `createdAt`.
- **Controlador REST `IncidentController` (`Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`):**
  - `GET /api/incidents/geocode/reverse?lat={lat}&lng={lng}`: Endpoint protegido para previsualizar la dirección postal y datos territoriales en el cliente.
  - `GET /api/incidents/my-incidents`: Endpoint protegido (`@GetMapping("/my-incidents")`) que recibe `status` (opcional), `page` y `size`, extrayendo la autenticación para llamar a `incidentService.getMyIncidents`.

### 4. Frontend: Capa de Servicios y API
- **`frontend/src/services/incidentService.js`:**
  - `getMyIncidents(params)`: Realiza petición `GET /api/incidents/my-incidents` con query params (`page`, `size`, `status`).
  - `reverseGeocode(latitude, longitude)`: Invoca `GET /api/incidents/geocode/reverse` para resolver la dirección al colocar el marcador.

### 5. Frontend: Formulario de Creación con Solo Lectura
- **`frontend/src/components/Map/CreateIncidentModal.jsx`:**
  - Al abrirse con nuevas coordenadas, solicita la dirección a `incidentService.reverseGeocode(...)`.
  - Renderiza campos bloqueados de solo lectura para Dirección, Barrio y Distrito.
  - Si los valores territoriales son nulos, renderiza el badge o texto informativo `"Sin asignar"`.

### 6. Frontend: Vista "Mis Incidencias" y Navegación
- **Página `frontend/src/pages/MyIncidentsPage.jsx` y `MyIncidentsPage.css`:**
  - Sección protegida con cabecera, filtros por estado (`TODOS`, `REPORTED`, `VALIDATED`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `REOPENED`, `CLOSED`, `REJECTED`) y botón de refresco.
  - Tabla o tarjetas responsive con: Título, Categoría, Estado (con badge de color), Dirección, Distrito, Fecha de creación y Fecha de última actualización.
  - Botón de acción "Ver detalle" que abre `IncidentDetailModal` con toda la información pública del reporte.
  - Controles de paginación (Página anterior / Página siguiente e indicador `Página X de Y`).
  - Estado vacío descriptivo con mensaje motivador y botón "Reportar incidencia en el mapa".
- **Enrutamiento en `frontend/src/App.jsx`:**
  - Registrar ruta `<Route path="/my-incidents" element={<ProtectedRoute><MyIncidentsPage /></ProtectedRoute>} />`.
  - Añadir enlace "Mis Incidencias" en la barra de navegación para usuarios autenticados.

---

## Decisiones

- **Integración centralizada con Datos Abiertos en el Backend (ver [ADR-06](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/docs/ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md)):**
  - _Justificación:_ Garantiza una política estricta de degradación controlada (timeout de 3s), previene problemas de CORS en frontend y asegura coherencia territorial tanto al previsualizar la dirección como al persistir la incidencia.
  - _Alternativas descartadas:_ Llamadas directas desde el navegador a la API externa (descartado por CORS y falta de control de resiliencia).
- **Campos de dirección, barrio y distrito de solo lectura:**
  - _Justificación:_ La posición geográfica marcada en el mapa es la fuente de verdad espacial; previene inconsistencias humanas en los nombres de calles y distritos.
  - _Alternativas descartadas:_ Permitir edición manual libre de la dirección (descartado por requisito explícito de usuario).
- **Uso de `Pageable` de Spring Data JPA y DTO canónico de paginación:**
  - _Justificación:_ Cumple el contrato `docs/api.md` (`GET /api/incidents/my-incidents`) de forma nativa y eficiente a nivel de base de datos (`LIMIT` / `OFFSET`).
- **Detalle de incidencias de acceso público para usuarios autenticados:**
  - _Justificación:_ Fomenta la transparencia comunitaria sin exponer auditorías ni datos personales internos de operadores.

---

## Riesgos

- **Latencia excesiva o caída del portal de Datos Abiertos de Málaga:**
  - _Mitigación:_ Configuración de timeout estricto de 3000 ms en el cliente HTTP del backend; activación de degradación controlada persistiendo `null` y mostrando `"Sin asignar"` sin abortar el alta.
- **Coordenadas fuera del término municipal de Málaga:**
  - _Mitigación:_ Validación geométrica de delimitación (bounding box) en capa de servicio y DTO antes de contactar con fuentes externas, respondiendo con HTTP 400 Bad Request y mensaje claro al usuario.
- **Navegación a páginas inexistentes (`page=999`):**
  - _Mitigación:_ Spring Data JPA responde con `content: []` y código 200 OK; el frontend gestiona limpiamente la lista vacía desactivando el botón de página siguiente.

---

## Estrategia de tests

- **Backend:**
  - **Pruebas unitarias de `MalagaOpenDataService`:** Simulación de respuestas exitosas (retorno de dirección, barrio y distrito), respuestas vacías (retorno de `null`), excepciones de timeout (`SocketTimeoutException` -> degradación controlada) y errores HTTP 500.
  - **Pruebas de persistencia (`@DataJpaTest`) en `IncidenciaRepository`:** Verificación de consultas `findByCitizenId` y `findByCitizenIdAndStatus` con ordenación cronológica descendente y paginación.
  - **Pruebas de servicio (`IncidentServiceTest`):** Verificación de asignación de `REPORTED` y `MEDIUM`, integración de datos territoriales, validación de coordenadas de Málaga y aislamiento de consultas por ciudadano.
  - **Pruebas de controlador (`@WebMvcTest` / MockMvc):** Verificación de endpoints `GET /api/incidents/my-incidents` y `GET /api/incidents/geocode/reverse` con token JWT, validación de 401 Unauthorized sin credenciales, y manejo de 404 ante ids inexistentes.
- **Frontend:**
  - **Pruebas unitarias con Vitest y Testing Library:**
    - `CreateIncidentModal`: Verificación de renderizado de campos de solo lectura y visualización de `"Sin asignar"` ante valores nulos.
    - `MyIncidentsPage`: Verificación del renderizado de la tabla/tarjetas de incidencias, cambio de página, filtrado por estado y estado vacío.
  - **Verificación de estilo y sintaxis:** `npm run lint` y `mvn test` en verde.
