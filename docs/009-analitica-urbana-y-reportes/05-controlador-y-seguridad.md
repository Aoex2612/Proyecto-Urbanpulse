# Tarea 5: Controlador REST y Seguridad RBAC en Backend

**Feature:** `009-analitica-urbana-y-reportes`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1 al RF-11, RF-14, RF-15, RF-16, RF-19, RF-20, RF-21  

---

## 1. Resumen de la Tarea

En esta tarea se ha implementado la capa de exposición HTTP REST y la política perimetral y metodológica de control de acceso basado en roles (RBAC) para el subsistema de analítica urbana y recomendación inteligente de UrbanPulse en Spring Boot 3 con Java 21:

### 1.1 Componente Controlador Desarrollado (`org.bulbaseros.controller.AnalyticsController`)
- Controlador REST decorado con `@RestController` y mapeado a la ruta base `/api/analytics`.
- Inyección de dependencias mediante constructor de:
  - `AnalyticsService`: Orquestador de KPIs operacionales, SLAs y agregaciones espaciales/temáticas.
  - `IntelligentRecommendationService`: Motor heurístico asistido con cálculo de confianza probabilística.
- Aplicación de defensa en profundidad mediante `@PreAuthorize("hasAnyRole('ANALYST', 'ADMIN', 'OPERATOR')")` a nivel de clase.

### 1.2 Reglas Perimetrales de Seguridad (`org.bulbaseros.config.SecurityConfig`)
- Inserción de la directiva de autorización en `securityFilterChain`:
  ```java
  .requestMatchers("/api/analytics/**").hasAnyRole("ANALYST", "ADMIN", "OPERATOR")
  ```
  ubicada antes de las reglas generales de autenticación.
- Actualización del Javadoc de la clase y del método de configuración para documentar la protección RBAC del módulo de analítica.

### 1.3 Adaptación en DTO de Filtrado (`org.bulbaseros.dto.AnalyticsFilterRequest`)
- Inclusión de la anotación `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)` en los atributos `startDate` y `endDate`, garantizando el enlace transparente de parámetros ISO-8601 vía query params en peticiones HTTP GET.

### 1.4 Configuración de Entorno de Pruebas (`pom.xml`)
- Inclusión de la dependencia `net.java.dev.jna:jna:5.13.0` con `<scope>test</scope>` para asegurar el correcto acoplamiento de ByteBuddy Agent / Mockito en entornos de ejecución con JRE modularizado.

---

## 2. Especificación de Endpoints y Contratos REST

### 2.1 `GET /api/analytics/summary` (RF-1 al RF-11)
Recupera el resumen cuantitativo integral de métricas urbanas, acuerdos de nivel de servicio (SLA) y desgloses agregados según los criterios de filtrado temporal y territorial suministrados.

- **Parámetros de consulta (opcionales):**
  - `preset`: Rango predefinido (`TODAY`, `LAST_7_DAYS`, `LAST_30_DAYS`, `LAST_3_MONTHS`, `LAST_6_MONTHS`, `LAST_YEAR`, `SPECIFIC_YEAR`).
  - `startDate`: Marca temporal ISO-8601 de inicio (ej. `2026-11-01T00:00:00Z`).
  - `endDate`: Marca temporal ISO-8601 de fin (ej. `2026-11-07T23:59:59Z`).
  - `year`: Año natural específico para preset `SPECIFIC_YEAR` (rango validado 2000-2100).
  - `district`: Nombre del distrito municipal (máximo 100 caracteres).
  - `neighborhood`: Nombre del barrio municipal (máximo 100 caracteres).

- **Respuestas HTTP:**
  - `200 OK`: Retorna `AnalyticsSummaryResponse` con `operationalMetrics`, `territorialMetrics`, `departmentMetrics`, `categoryMetrics`, `assistantPerformance`, `filteredIncidents`, `appliedFilters` y `generatedAt`.
  - `400 Bad Request`: Si algún parámetro viola Bean Validation (ej. año fuera del rango 2000-2100).
  - `401 Unauthorized`: Token JWT ausente, expirado o con firma inválida.
  - `403 Forbidden`: Usuario autenticado con rol sin privilegios analíticos (`CITIZEN` o `TECHNICIAN`).

### 2.2 `GET /api/analytics/recommend-department` (RF-14, RF-15, RF-16)
Genera la recomendación asistida del departamento municipal idóneo para triaje y asignación de incidencias.

- **Parámetros de consulta (opcionales):**
  - `incidentId`: Identificador único de una incidencia ya persistida.
  - `title`: Título textual de la incidencia.
  - `description`: Detalle descriptivo de la incidencia.
  - `category`: Categoría inicial clasificada.

- **Lógica de resolución:**
  - Si `incidentId != null`: Invoca `intelligentRecommendationService.recommendDepartment(incidentId)`.
  - En caso contrario: Invoca `intelligentRecommendationService.recommendDepartment(title, description, category)`.

- **Respuestas HTTP:**
  - `200 OK`: Retorna `DepartmentRecommendationResponse` conteniendo `suggestedDepartmentId`, `suggestedDepartmentName`, `confidencePercentage`, `isLowConfidenceWarning` y `reasoning`.
  - `401 Unauthorized`: Token JWT ausente o inválido.
  - `403 Forbidden`: Rol sin privilegios (`CITIZEN` o `TECHNICIAN`).
  - `404 Not Found`: Si se suministró un `incidentId` inexistente (`IncidentNotFoundException`).

---

## 3. Matriz de Autorización RBAC y Defensa en Profundidad

| Rol en UrbanPulse | Acceso a `/api/analytics/summary` | Acceso a `/api/analytics/recommend-department` | Código HTTP Esperado |
| :--- | :---: | :---: | :---: |
| **ANONYMOUS** (Sin token) | ❌ Denegado | ❌ Denegado | **401 Unauthorized** |
| **CITIZEN** | ❌ Denegado | ❌ Denegado | **403 Forbidden** |
| **TECHNICIAN** | ❌ Denegado | ❌ Denegado | **403 Forbidden** |
| **OPERATOR** | ✅ Permitido | ✅ Permitido | **200 OK** |
| **ADMIN** | ✅ Permitido | ✅ Permitido | **200 OK** |
| **ANALYST** | ✅ Permitido | ✅ Permitido | **200 OK** |

### Defensa en Profundidad:
1. **Barrera Perimetral (`SecurityConfig`):** Inspecciona y filtra las rutas `/api/analytics/**` antes de que la petición ingrese al contexto del despachador MVC.
2. **Barrera a Nivel de Controlador (`@PreAuthorize`):** Intercepta la llamada al proxy del controlador validando la presencia de las autoridades requeridas (`ROLE_ANALYST`, `ROLE_ADMIN`, `ROLE_OPERATOR`), blindando el endpoint incluso ante eventuales reconfiguraciones de la cadena de filtros HTTP.

---

## 4. Pruebas Automatizadas y Verificación

### 4.1 Suite `AnalyticsControllerTest` (15 tests en 3 clases anidadas)
- **`SummarySecurityTests` (6 tests):**
  - Petición anónima responde 401 Unauthorized.
  - Rol `CITIZEN` responde 403 Forbidden.
  - Rol `TECHNICIAN` responde 403 Forbidden.
  - Rol `ANALYST` responde 200 OK y verifica el payload JSON estructurado.
  - Rol `ADMIN` responde 200 OK.
  - Rol `OPERATOR` responde 200 OK.
- **`SummaryFilteringTests` (2 tests):**
  - Enlace de parámetros válidos (`preset`, `district`, `neighborhood`) mediante `ArgumentCaptor`.
  - Año inválido (`year=1995`) responde 400 Bad Request por Bean Validation.
- **`RecommendationEndpointTests` (7 tests):**
  - Petición anónima responde 401 Unauthorized.
  - Rol `CITIZEN` responde 403 Forbidden.
  - Rol `TECHNICIAN` responde 403 Forbidden.
  - Rol `OPERATOR` con `incidentId` retorna 200 OK y recomendación asociada.
  - Rol `OPERATOR` con atributos de texto directo (`title`, `description`, `category`) retorna 200 OK.
  - Advertencia explícita de baja confianza (`isLowConfidenceWarning = true`) con rol `ANALYST`.
  - `incidentId` inexistente lanza `IncidentNotFoundException` y responde 404 Not Found estructurado.

### 4.2 Suite `SecurityConfigTest` (19 tests)
- Ampliada con 6 casos específicos para verificar el control de acceso perimetral sobre `/api/analytics/**` frente a usuarios anónimos y roles `CITIZEN`, `TECHNICIAN`, `ANALYST`, `ADMIN` y `OPERATOR`.

### 4.3 Resultados Globales de Ejecución Maven:
```
Tests run: 577, Failures: 0, Errors: 0, Skipped: 1
Total time: 24.087 s
BUILD SUCCESS
```
