# Tarea 4: DTOs y Controlador de Triaje de Incidencias en Backend

**Feature:** `002-autenticacion-y-control-de-acceso-rbac`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-5, RF-6  

---

## 1. Resumen de la Tarea

En esta tarea se implementaron los artefactos de datos y el controlador representativo de triaje de incidencias (`IncidentController`), completando la superficie perimetral de control de acceso basada en roles (RBAC) en el Backend de UrbanPulse. Se expuso formalmente el endpoint `PATCH /api/incidents/{id}/status`, restringido de manera estricta a usuarios que cuenten con roles operativos autorizados (`OPERATOR` y `ADMIN`).

Asimismo, se crearon los DTOs de petición y respuesta con Bean Validation y Javadoc exhaustivo, junto con una suite completa de pruebas de integración de seguridad (`IncidentControllerSecurityTest`) y pruebas unitarias de DTO (`IncidentDtoTest`), verificando:
1. El rechazo inmediato con HTTP 401 Unauthorized ante peticiones sin autenticación.
2. El rechazo con HTTP 403 Forbidden ante peticiones realizadas por ciudadanos (`CITIZEN`).
3. La autorización y procesamiento exitoso con HTTP 200 OK y DTO `IncidentStatusResponse` para operadores (`OPERATOR`) y administradores (`ADMIN`).
4. La respuesta HTTP 400 Bad Request estructurada ante estados nulos o inválidos.

---

## 2. Subtareas implementadas

### 2.1 DTOs de Triaje de Incidencias (Subtarea 4.1)
- **`UpdateIncidentStatusRequest` (`Backend/src/main/java/org/bulbaseros/dto/UpdateIncidentStatusRequest.java`):**
  - Campo `status` (`EstadoIncidencia`) validado con `@NotNull(message = "El estado de la incidencia es obligatorio")`.
  - Campo `comment` (`String`) opcional para justificaciones o anotaciones operativas de triaje.
  - Constructores sobrecargados (vacío para deserialización Jackson, con enum `EstadoIncidencia`, con string de conveniencia y completo con comentario).
  - Métodos accesores (getters y setters) con Javadoc exhaustivo.
- **`IncidentStatusResponse` (`Backend/src/main/java/org/bulbaseros/dto/IncidentStatusResponse.java`):**
  - Campos: `id` (`Long`), `status` (`EstadoIncidencia`), `message` (`String`) y `updatedAt` (`OffsetDateTime`).
  - Constructores parametrizados y por defecto con inicialización automática de `OffsetDateTime.now()`.
  - Documentación Javadoc completa en clase, campos y métodos accesores.
- **`IncidentDtoTest` (`Backend/src/test/java/org/bulbaseros/dto/IncidentDtoTest.java`):**
  - 4 pruebas unitarias que validan el motor de Bean Validation (`Validator`), constructores, getters y setters de ambos DTOs.

### 2.2 Controlador de Incidencias (Subtarea 4.2)
- **`IncidentController` (`Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`):**
  - Anotado con `@RestController` y mapeado en `/api/incidents`.
  - Endpoint `@PatchMapping("/{id}/status")` que recibe `@PathVariable Long id` y `@Valid @RequestBody UpdateIncidentStatusRequest request`.
  - Retorna `ResponseEntity.ok(IncidentStatusResponse)` con código HTTP 200 OK, el identificador de la incidencia, el nuevo estado confirmado, mensaje de confirmación y marca de tiempo `OffsetDateTime`.
  - Documentado con Javadoc exhaustivo cubriendo propósitos de negocio, parámetros, respuestas y requisitos RBAC asociados.

### 2.3 Pruebas de Integración y Seguridad (Subtarea 4.3)
- **`IncidentControllerSecurityTest` (`Backend/src/test/java/org/bulbaseros/controller/IncidentControllerSecurityTest.java`):**
  - Configurado con `@SpringBootTest` y `@AutoConfigureMockMvc`.
  - 9 pruebas de integración cubriendo:
    - **HTTP 401 Unauthorized:** Petición no autenticada a `PATCH /api/incidents/1/status` retorna 401 con DTO `ErrorResponse` (`error: "Unauthorized"`, `message: "Token de autenticación ausente o inválido"`, `path`).
    - **HTTP 403 Forbidden (`CITIZEN` con mock):** Petición con `@WithMockUser(roles = "CITIZEN")` retorna 403 con DTO `ErrorResponse` (`error: "Forbidden"`, `message: "Acceso denegado: permisos insuficientes"`).
    - **HTTP 403 Forbidden (`CITIZEN` con JWT real):** Petición con cabecera `Authorization: Bearer <token>` generado por `JwtService` con rol `CITIZEN` retorna 403 Forbidden.
    - **HTTP 200 OK (`OPERATOR` con mock):** Petición con `@WithMockUser(roles = "OPERATOR")` retorna 200 OK con confirmación `IncidentStatusResponse`.
    - **HTTP 200 OK (`OPERATOR` con JWT real):** Petición con cabecera Bearer y token JWT emitido para un `OPERATOR` retorna 200 OK con `id`, `status: "ASSIGNED"` y mensaje descriptivo.
    - **HTTP 200 OK (`ADMIN` con mock):** Petición con `@WithMockUser(roles = "ADMIN")` retorna 200 OK con confirmación `IncidentStatusResponse`.
    - **HTTP 200 OK (`ADMIN` con JWT real):** Petición con cabecera Bearer y token JWT emitido para un `ADMIN` retorna 200 OK con `id`, `status: "CLOSED"` y mensaje descriptivo.
    - **HTTP 400 Bad Request:** Petición de un operador con payload inválido (`{"status": null}`) genera violación de Bean Validation y retorna 400 Bad Request con `ErrorResponse`.
    - **Invocación Unitaria:** Ejecución directa del método `updateIncidentStatus` de `IncidentController` garantizando respuesta correcta desacoplada.

---

## 3. Criterios de Aceptación y Requisitos Funcionales Verificados

| Requisito | Descripción | Estado | Evidencia de Validación |
| :--- | :--- | :---: | :--- |
| **RF-1** | Peticiones a endpoints protegidos sin token retornan HTTP 401 Unauthorized estructurado. | Cumplido | `shouldRejectUnauthenticatedRequestWith401` en `IncidentControllerSecurityTest` |
| **RF-5** | Peticiones con rol `CITIZEN` a `PATCH /api/incidents/{id}/status` son rechazadas con HTTP 403 Forbidden (`ErrorResponse`). | Cumplido | `shouldRejectCitizenUserWith403` y `shouldRejectRealJwtTokenWithCitizenRoleWith403` |
| **RF-6** | Peticiones con rol `OPERATOR` o `ADMIN` son autorizadas y retornan HTTP 200 OK (`IncidentStatusResponse`). | Cumplido | `shouldAuthorizeOperatorUserWith200`, `shouldAuthorizeAdminUserWith200`, `shouldAuthorizeRealJwtTokenWithOperatorRoleWith200` y `shouldAuthorizeRealJwtTokenWithAdminRoleWith200` |

---

## 4. Resultados de la Suite de Pruebas

Se ejecutó la suite completa de pruebas unitarias y de integración de Backend con Maven:

```bash
/home/joalts8/Descargas/idea-IU-262.10315.125/plugins/maven-plugin/lib/maven3/bin/mvn -f Backend/pom.xml test
```

**Resultado:**
- **Tests ejecutados:** 116
- **Fallos (Failures):** 0
- **Errores (Errors):** 0
- **Omitidos (Skipped):** 0
- **Tiempo total:** 5.926 s
- **Estado del build:** `BUILD SUCCESS`
