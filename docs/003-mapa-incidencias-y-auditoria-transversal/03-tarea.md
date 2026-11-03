# Tarea 3: Endpoints REST y Reglas de Seguridad RBAC en Backend

**Feature:** `003-mapa-incidencias-y-auditoria-transversal`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-10, RF-13, RF-14, RF-15, RF-16, RF-17  

---

## 1. Resumen de la Tarea

En esta tarea se expusieron los endpoints REST para la gestion de incidencias cartograficas y la auditoria transversal inmutable, protegiendo perimetralmente el acceso mediante reglas declarativas de control de acceso basado en roles (RBAC) en Spring Security:

1. Se actualizo `Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`, inyectando `IncidentService` por constructor e implementando cinco endpoints protegidos:
   - `GET /api/incidents`: retorna `ResponseEntity<List<IncidentResponse>>` con codigo 200 OK invocando `incidentService.getActiveIncidents()`.
   - `GET /api/incidents/{id}`: retorna `ResponseEntity<IncidentResponse>` con codigo 200 OK invocando `incidentService.getIncidentById(id)`.
   - `POST /api/incidents`: recibe `@Valid @RequestBody CreateIncidentRequest request` y `Authentication authentication`, extrae el email del emisor con `authentication.getName()`, invoca `incidentService.createIncident(request, email)` y retorna `ResponseEntity.status(HttpStatus.CREATED).body(response)`.
   - `PATCH /api/incidents/{id}/status`: recibe `@PathVariable Long id`, `@Valid @RequestBody UpdateIncidentStatusRequest request` y `Authentication authentication`, extrae el email del operador o administrador con `authentication.getName()`, invoca `incidentService.updateIncidentStatus(id, request, email)` y retorna `ResponseEntity.ok(response)`.
   - `GET /api/incidents/{id}/history`: recibe `@PathVariable Long id`, invoca `incidentService.getIncidentHistory(id)` y retorna `ResponseEntity.ok(historyList)`.
2. Se actualizo `Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java` configurando en el `SecurityFilterChain` la precedencia estricta de matchers:
   - `.requestMatchers("/api/auth/**", "/h2-console/**").permitAll()`
   - `.requestMatchers(HttpMethod.GET, "/api/incidents/*/history").hasRole("ADMIN")`
   - `.requestMatchers(HttpMethod.PATCH, "/api/incidents/*/status").hasAnyRole("OPERATOR", "ADMIN")`
   - `.requestMatchers("/api/incidents/**").authenticated()`
   - `.anyRequest().authenticated()`
3. Se amplio la suite de pruebas de integracion y seguridad en `Backend/src/test/java/org/bulbaseros/controller/IncidentControllerSecurityTest.java` alcanzando 23 casos de prueba que verifican respuestas 200, 201, 400, 401, 403 y 404 bajo perfiles `ANONYMOUS`, `CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ANALYST` y `ADMIN`, tanto con `@WithMockUser` como con tokens JWT firmados reales.
4. Se ejecuto la suite completa de tests de Backend (`mvn clean test`), logrando 166 tests pasados con 0 fallos, 0 errores y 0 omitidos (100% en verde).
5. Se ejecuto el linter del frontend (`npm run lint`), validando 0 errores y advertencias.

---

## 2. Subtareas Implementadas

### 2.1 Controlador `IncidentController` (Subtarea 3.1)
- **`Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`:**
  - Inyeccion obligatoria de `IncidentService` por constructor.
  - Implementacion de `GET /api/incidents` para suministrar marcadores activos al mapa cartografico.
  - Implementacion de `GET /api/incidents/{id}` para consultar datos detallados de una incidencia.
  - Implementacion de `POST /api/incidents` retornando HTTP 201 Created y vinculando la incidencia al ciudadano autenticado extraido de `Authentication`.
  - Implementacion de `PATCH /api/incidents/{id}/status` retornando HTTP 200 OK y delegando la transicion atómica con auditoria al servicio.
  - Implementacion de `GET /api/incidents/{id}/history` retornando HTTP 200 OK con la coleccion cronologica de cambios de estado o 404 si la incidencia no existe.
  - Documentacion Javadoc exhaustiva en clase, campos, constructor y metodos con descripciones de parametros (`@param`), retornos (`@return`) y excepciones.

### 2.2 Seguridad Perimetral y RBAC en `SecurityConfig` (Subtarea 3.2)
- **`Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java`:**
  - Configuracion explicita de las reglas de autorizacion en `securityFilterChain`:
    - `GET /api/incidents/*/history` restringido a usuarios con rol `ADMIN`.
    - `PATCH /api/incidents/*/status` restringido a roles operativos `OPERATOR` y `ADMIN`.
    - `/api/incidents/**` accesible para cualquier usuario autenticado (`CITIZEN`, `OPERATOR`, `ADMIN`, etc.).
  - Ordenacion de matchers situando las reglas mas especificas (`/history` y `/status`) antes del comodin general `/api/incidents/**`.
  - Mantenimiento de la arquitectura sin estado (stateless), desactivacion de CSRF e integracion del filtro `JwtAuthenticationFilter`.

### 2.3 Suite de Pruebas de Integracion y Seguridad (Subtarea 3.3)
- **`Backend/src/test/java/org/bulbaseros/controller/IncidentControllerSecurityTest.java`:**
  - Suite de 23 pruebas exhaustivas utilizando `@SpringBootTest` y `@AutoConfigureMockMvc`:
    - **Rechazo no autenticado (401 Unauthorized):**
      - Peticion anonima a `GET /api/incidents` responde 401 con `ErrorResponse`.
      - Peticion anonima a `POST /api/incidents` responde 401 con `ErrorResponse`.
      - Peticion anonima a `GET /api/incidents/{id}/history` responde 401 con `ErrorResponse`.
      - Peticion anonima a `PATCH /api/incidents/{id}/status` responde 401 con `ErrorResponse`.
    - **Denegacion de privilegios RBAC (403 Forbidden):**
      - Peticion con rol `CITIZEN` a `PATCH /api/incidents/{id}/status` responde 403.
      - Peticion con rol `TECHNICIAN` a `PATCH /api/incidents/{id}/status` responde 403.
      - Peticion con rol `ANALYST` a `PATCH /api/incidents/{id}/status` responde 403.
      - Peticion con rol `CITIZEN` a `GET /api/incidents/{id}/history` responde 403.
      - Peticion con rol `OPERATOR` a `GET /api/incidents/{id}/history` responde 403.
      - Peticion con rol `TECHNICIAN` a `GET /api/incidents/{id}/history` responde 403.
      - Peticion con rol `ANALYST` a `GET /api/incidents/{id}/history` responde 403.
    - **Autorizacion de roles operativos y administrativos (200 OK / 201 Created):**
      - Peticion con rol `OPERATOR` a `PATCH /api/incidents/{id}/status` responde 200 OK.
      - Peticion con rol `ADMIN` a `PATCH /api/incidents/{id}/status` responde 200 OK.
      - Peticion con rol `ADMIN` a `GET /api/incidents/{id}/history` responde 200 OK.
      - Peticion con rol `CITIZEN` a `POST /api/incidents` responde 201 Created.
      - Peticion con rol `CITIZEN` a `GET /api/incidents` responde 200 OK.
      - Peticion con rol `CITIZEN` a `GET /api/incidents/{id}` responde 200 OK.
    - **Gestion de incidencias inexistentes (404 Not Found):**
      - Peticion con rol `ADMIN` a `GET /api/incidents/999/history` responde 404 Not Found con mensaje descriptivo estructurado en `ErrorResponse`.
    - **Pruebas perimetrales con tokens JWT reales:**
      - Token JWT de `CITIZEN` a `PATCH /status` responde 403 Forbidden.
      - Token JWT de `OPERATOR` a `PATCH /status` responde 200 OK.
      - Token JWT de `ADMIN` a `PATCH /status` y `GET /history` responde 200 OK.
    - **Validacion de payloads Bean Validation (400 Bad Request):**
      - Estado nulo en `PATCH /status` retorna HTTP 400 Bad Request.
    - **Invocacion unitaria directa:**
      - Invocacion directa desacoplada del controlador verificando contratos y respuestas HTTP.

---

## 3. Archivos Modificados y Creados

| Archivo | Tipo de Cambio | Descripcion |
|---------|----------------|-------------|
| `Backend/src/main/java/org/bulbaseros/controller/IncidentController.java` | Modificado | Inyeccion de `IncidentService` y exposicion de endpoints REST completos con Javadoc. |
| `Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java` | Modificado | Declaracion de matchers RBAC especificos para `/history`, `/status` y `/api/incidents/**`. |
| `Backend/src/test/java/org/bulbaseros/controller/IncidentControllerSecurityTest.java` | Modificado | Suite integral de 23 tests de seguridad RBAC, validacion y contratos REST. |
| `features/003-mapa-incidencias-y-auditoria-transversal/tasks.md` | Modificado | Marcada la Tarea 3 y sus subtareas 3.1, 3.2 y 3.3 como completadas `[x]`. |
| `docs/003-mapa-incidencias-y-auditoria-transversal/03-tarea.md` | Creado | Documentacion tecnica detallada de la Tarea 3. |

---

## 4. Validacion y Resultados de Pruebas

### 4.1 Tests Backend (`mvn clean test`)

```bash
mvn clean test
```

**Resultado:**
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.AppTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.config.SecurityConfigTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.controller.AuthControllerTest
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.controller.IncidentControllerSecurityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.dto.CreateIncidentRequestTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.dto.IncidentDtoTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.dto.IncidentResponseTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.dto.StatusChangeResponseTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.dto.ValidationDtoTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.entity.UsuarioTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.exception.GlobalExceptionHandlerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.repository.CambiosEstadoRepositoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.repository.IncidenciaRepositoryTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.repository.UserRepositoryTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.security.CustomAccessDeniedHandlerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.security.JwtAuthenticationEntryPointTest
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.security.JwtAuthenticationFilterTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.security.JwtServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.service.AuthServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in org.bulbaseros.service.IncidentServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 166, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 4.2 Linter Frontend (`npm run lint`)

```bash
npm --prefix frontend run lint
```

**Resultado:**
```
> frontend@0.0.0 lint
> eslint .
Exit Code: 0 (0 errores, 0 advertencias)
```

---

## 5. Proximos Pasos

Continuar con la **Tarea 4: Dependencias Cartográficas y Capa de Servicio en Frontend**:
- Instalar dependencias `leaflet` y `react-leaflet` en `frontend/package.json` e importar `leaflet/dist/leaflet.css`.
- Configurar la resolucion de iconos y assets de marcadores de Leaflet para Vite.
- Desarrollar el modulo `incidentService.js` en `frontend/src/services/` para conectar el cliente web con los nuevos endpoints REST del backend.
