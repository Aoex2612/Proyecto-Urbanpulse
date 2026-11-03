# Tarea 2: Logica de Negocio Transaccional en `IncidentService`

**Feature:** `003-mapa-incidencias-y-auditoria-transversal`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-10, RF-13, RF-14, RF-15, RF-16  

---

## 1. Resumen de la Tarea

En esta tarea se desarrollo la capa de servicio y logica de negocio transaccional para la gestion de incidencias urbanas y la auditoria transversal inmutable:

1. Se creo e implemento el servicio de negocio `Backend/src/main/java/org/bulbaseros/service/IncidentService.java`, inyectando `IncidenciaRepository`, `CambiosEstadoRepository` y `UserRepository`.
2. Se definio la excepcion especifica `IncidentNotFoundException` (`@ResponseStatus(HttpStatus.NOT_FOUND)`) y se integro en `GlobalExceptionHandler` junto con el soporte estructurado para `ResponseStatusException`.
3. Se implementaron los cinco metodos de negocio especificados en el diseno técnico:
   - `getActiveIncidents()`: recupera todas las incidencias no terminadas (excluyendo estados finales `CLOSED` y `REJECTED`) transformandolas a lista de `IncidentResponse` para su consumo cartografico.
   - `getIncidentById(Long id)`: busca la incidencia por su identificador primario lanzando `IncidentNotFoundException` (HTTP 404) si no existe.
   - `createIncident(CreateIncidentRequest request, String userEmail)`: localiza al ciudadano emisor a traves del correo electronico del token JWT, inicializa el estado con `EstadoIncidencia.REPORTED` y la prioridad con `PrioridadIncidencia.MEDIUM`, asigna marcas temporales actuales y persiste la entidad retornando `IncidentResponse`.
   - `updateIncidentStatus(Long id, UpdateIncidentStatusRequest request, String userEmail)`: metodo transaccional atomico (`@Transactional`) que valida la existencia de la incidencia (HTTP 404), busca al operador o administrador actuante por su email, actualiza el estado de la incidencia y registra de forma indivisible una entrada inmutable de auditoria en `CambiosEstado` con el estado origen, el estado destino, el usuario responsable y el comentario o motivo de la transicion.
   - `getIncidentHistory(Long id)`: verifica en primer lugar la existencia de la incidencia (lanzando `IncidentNotFoundException` HTTP 404 si no existe) y recupera la coleccion cronologica de cambios de estado ordenada ascendentemente mediante `cambiosEstadoRepository.findByIncidentIdOrderByCreatedAtAsc(id)`.
4. Se desarrollo una suite exhaustiva de 14 pruebas unitarias con Mockito en `Backend/src/test/java/org/bulbaseros/service/IncidentServiceTest.java`, verificando filtrado de activas, creacion con metadatos por defecto, atomicidad en la transicion de estados y auditoria, control de excepciones 404 e historial ordenado.
5. Se actualizaron las pruebas unitarias en `GlobalExceptionHandlerTest` para asegurar el manejo de `IncidentNotFoundException` y `ResponseStatusException`.
6. Se ejecutaron todos los tests del backend con `mvn clean test`, alcanzando 152 pruebas exitosas con 0 fallos, 0 errores y 0 omitidos (100% en verde).

---

## 2. Subtareas implementadas

### 2.1 Servicio `IncidentService` (Subtareas 2.1, 2.2, 2.3 y 2.4)
- **`Backend/src/main/java/org/bulbaseros/service/IncidentService.java`:**
  - Componente `@Service` con inyeccion por constructor de `IncidenciaRepository`, `CambiosEstadoRepository` y `UserRepository`.
  - `getActiveIncidents()`: marcado con `@Transactional(readOnly = true)`, invoca `findByStatusNotIn(List.of(EstadoIncidencia.CLOSED, EstadoIncidencia.REJECTED))` y mapea a `IncidentResponse`.
  - `getIncidentById(Long id)`: validacion de identificador no nulo, recuperacion mediante `findById(id)` o lanzamiento de `IncidentNotFoundException`.
  - `createIncident(CreateIncidentRequest request, String userEmail)`: marcado con `@Transactional`, normalizacion de correo electronico, asociacion del usuario emisor, establecimiento de estado `REPORTED`, prioridad `MEDIUM` y persistencia en base de datos.
  - `updateIncidentStatus(Long id, UpdateIncidentStatusRequest request, String userEmail)`: marcado con `@Transactional`, recuperacion de la incidencia existente, recuperacion del usuario autorizador, transicion de estado en `Incidencia` e insercion sincronizada en `CambiosEstado`.
  - `getIncidentHistory(Long id)`: marcado con `@Transactional(readOnly = true)`, comprobacion de existencia con `existsById(id)` y obtencion del historial cronologico mapeado a `StatusChangeResponse`.
  - Documentacion Javadoc completa en clase, campos, constructor y metodos incluyendo etiquetas `@param`, `@return` y `@throws`.

### 2.2 Excepcion `IncidentNotFoundException` y Manejo Global (Subtareas 2.3 y 2.4)
- **`Backend/src/main/java/org/bulbaseros/exception/IncidentNotFoundException.java`:**
  - Excepcion en tiempo de ejecucion anotada con `@ResponseStatus(HttpStatus.NOT_FOUND)`.
  - Constructores con mensaje libre y constructor por identificador de incidencia.
  - Documentacion Javadoc completa.
- **`Backend/src/main/java/org/bulbaseros/exception/GlobalExceptionHandler.java`:**
  - Metodo `@ExceptionHandler(IncidentNotFoundException.class)` retornando HTTP 404 y `ErrorResponse`.
  - Metodo `@ExceptionHandler(ResponseStatusException.class)` extrayendo el codigo HTTP dinamico y justificacion.

### 2.3 Pruebas Unitarias de Servicio (Subtarea 2.5)
- **`Backend/src/test/java/org/bulbaseros/service/IncidentServiceTest.java`:**
  - 14 pruebas unitarias implementadas con Mockito (`@ExtendWith(MockitoExtension.class)`):
    - `shouldReturnActiveIncidentsExcludingClosedAndRejected`: valida exclusion de CLOSED y REJECTED y mapeo a DTO.
    - `shouldGetIncidentByIdSuccessfully`: valida recuperacion correcta de incidencia por id.
    - `shouldThrowIncidentNotFoundExceptionWhenIncidentDoesNotExistOnGetById`: valida lanzamiento de 404 ante id inexistente.
    - `shouldThrowIllegalArgumentExceptionWhenIdIsNullOnGetById`: valida proteccion contra id nulo.
    - `shouldCreateIncidentSuccessfully`: valida asignacion de ciudadano, estado inicial REPORTED, prioridad MEDIUM y marcas temporales.
    - `shouldThrowResponseStatusExceptionWhenUserNotFoundOnCreate`: valida rechazo 404 si el usuario autenticado no existe en BD.
    - `shouldThrowIllegalArgumentExceptionOnCreateIncidentWithInvalidArgs`: valida proteccion contra solicitud nula o email vacio.
    - `shouldUpdateIncidentStatusAndCreateAuditChangeAtomically`: valida captura mediante `ArgumentCaptor` de la entidad `CambiosEstado` verificando estado anterior ("REPORTED"), nuevo ("IN_PROGRESS"), motivo justificado y usuario responsable.
    - `shouldThrowIncidentNotFoundExceptionWhenIncidentDoesNotExistOnUpdateStatus`: valida error 404 e inhibicion de persistencia en auditoria.
    - `shouldThrowResponseStatusExceptionWhenActorUserDoesNotExistOnUpdateStatus`: valida error 404 si el usuario actor no existe.
    - `shouldThrowIllegalArgumentExceptionOnUpdateStatusWithInvalidArgs`: valida proteccion ante parametros invalidos.
    - `shouldReturnIncidentHistoryChronologically`: valida ordenacion cronologica y mapeo a `StatusChangeResponse`.
    - `shouldThrowIncidentNotFoundExceptionWhenIncidentDoesNotExistOnGetHistory`: valida verificacion previa de existencia y lanzamiento de 404.
    - `shouldThrowIllegalArgumentExceptionWhenIdIsNullOnGetHistory`: valida proteccion contra id nulo.

### 2.4 Pruebas Unitarias del Manejador de Excepciones
- **`Backend/src/test/java/org/bulbaseros/exception/GlobalExceptionHandlerTest.java`:**
  - Pruebas para `handleIncidentNotFoundException` verificando retorno HTTP 404, status 404, path y mensaje explicativo.
  - Pruebas para `handleResponseStatusException` verificando codigo de estado y razon.

---

## 3. Archivos Modificados y Creados

| Archivo | Tipo de Cambio | Descripcion |
|---------|----------------|-------------|
| `Backend/src/main/java/org/bulbaseros/service/IncidentService.java` | Creado | Servicio principal con la logica de negocio transaccional y auditoria. |
| `Backend/src/main/java/org/bulbaseros/exception/IncidentNotFoundException.java` | Creado | Excepcion 404 para incidencias no encontradas. |
| `Backend/src/main/java/org/bulbaseros/exception/GlobalExceptionHandler.java` | Modificado | Anadidos manejadores para `IncidentNotFoundException` y `ResponseStatusException`. |
| `Backend/src/test/java/org/bulbaseros/service/IncidentServiceTest.java` | Creado | Suite de 14 pruebas unitarias con Mockito para `IncidentService`. |
| `Backend/src/test/java/org/bulbaseros/exception/GlobalExceptionHandlerTest.java` | Modificado | Anadidos tests unitarios para las nuevas excepciones 404. |
| `features/003-mapa-incidencias-y-auditoria-transversal/tasks.md` | Modificado | Marcada la Tarea 2 y todas sus subtareas como completadas `[x]`. |
| `docs/003-mapa-incidencias-y-auditoria-transversal/02-tarea.md` | Creado | Documentacion tecnica detallada de la Tarea 2. |

---

## 4. Validacion y Resultados de Pruebas

Se ejecuto la suite de tests completa del backend mediante Maven:
```bash
mvn clean test
```

**Resultado:**
```
[INFO] Running org.bulbaseros.AppTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.config.SecurityConfigTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.controller.AuthControllerTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.controller.IncidentControllerSecurityTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.dto.CreateIncidentRequestValidationTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.dto.IncidentDtoTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.dto.IncidentResponseTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.dto.StatusChangeResponseTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.dto.ValidationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.entity.UsuarioTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.exception.GlobalExceptionHandlerTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.repository.CambiosEstadoRepositoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.repository.IncidenciaRepositoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.repository.UserRepositoryTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.security.CustomAccessDeniedHandlerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.security.JwtAuthenticationEntryPointTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.security.JwtAuthenticationFilterTest
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.security.JwtServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.service.AuthServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.service.IncidentServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 152, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 5. Proximo Paso

- **Tarea 3: Endpoints REST y Reglas de Seguridad RBAC en Backend:**
  - Actualizar `IncidentController` inyectando `IncidentService` y exponiendo los endpoints `GET /api/incidents`, `GET /api/incidents/{id}`, `POST /api/incidents`, `PATCH /api/incidents/{id}/status` y `GET /api/incidents/{id}/history`.
  - Configurar las directivas de seguridad en `SecurityConfig` para proteger endpoints con las restricciones de rol pertinentes (`ADMIN`, `OPERATOR`, `CITIZEN`).
  - Desarrollar las pruebas de integracion y seguridad en `IncidentControllerSecurityTest`.
