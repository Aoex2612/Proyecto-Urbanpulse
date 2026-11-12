# QA Note — Feature 010: Identificadores Publicos UUID y Versionado API v2 (Issue #59)

**Fecha:** 12/11/2026  
**Rama:** `refactor/59-uso-id-publico`  
**Estado de Validacion:** APROBADA (EXITO) :white_check_mark:  
**Responsable QA:** Subagente de Validacion SDD  

---

## 1. Resumen de la Validacion y Veredicto

Se ha auditado tecnicamente la resolucion de la **Issue #59** (*"[Refactor]: Uso de ID publico en vez de el privado de la BD"*), correspondiente a la feature **010-identificadores-publicos-y-api-v2**. La refactorizacion mitiga la vulnerabilidad de enumeracion y referencia directa a objetos insegura (IDOR) introduciendo identificadores universales publicos (`UUID v4`) en la frontera externa del sistema, manteniendo las claves primarias numericas internamente para preservar el rendimiento relacional de PostgreSQL, e implementando un esquema de doble versionado REST (`/api/v1` retrocompatible y `/api/v2` canónico).

### Veredicto: **APROBADA CON EXITO (100% de cumplimiento)**
- **Requisitos Funcionales (RF-UUID-BD, RF-API-V2, RF-RETROCOMPATIBILIDAD-V1, RF-VALIDACION-UUID, RF-FRONTEND-V2):** 100% cubiertos y validados.
- **Requisitos No Funcionales (RNF):** Seguridad por diseno, rendimiento en base de datos preservado, tolerancia a fallos en UI y cero regresiones funcionales en tests heredados.
- **Linters:** Frontend ESLint sin errores (`0 errors, 0 warnings`).
- **Suites de Pruebas:**
  - **Frontend (Vitest v8):** 35 suites de test, **270 tests pasados con exito** (0 fallos).
  - **Backend (Maven / JUnit 5):** **704 tests pasados con exito** (0 errores, 0 fallos, 1 omitido condicionalmente `@EnabledIfPostgresAvailable`).

---

## 2. Tabla de Trazabilidad de Requisitos Funcionales y Cobertura de Tests

| Requisito | Descripcion del Requisito | Suites / Tests que lo cubren | Resultado |
|---|---|---|:---:|
| **RF-UUID-BD** | Clave publica `public_id` UUID unica, no nula y generada automaticamente (`gen_random_uuid()` en BD y `@PrePersist` en JPA) en todas las entidades del dominio. | Suite completa JPA y persistencia | **PASS** |
| **RF-API-V2** | Publicacion de la API v2 (`/api/v2/...`) donde los identificadores expuestos en JSON y rutas son cadenas UUID String, blindando IDs de BD. | `IncidentControllerV2Test`, `DepartmentControllerV2Test`, `NotificationControllerV2Test` | **PASS** |
| **RF-RETROCOMPATIBILIDAD-V1** | Coexistencia retrocompatible de endpoints `/api/v1/...` y `/api/...` con IDs numericos `Long` para no romper suites ni clientes heredados. | `IncidentControllerTest`, `DepartmentControllerTest`, `NotificationControllerTest` | **PASS** |
| **RF-VALIDACION-UUID** | Manejo estructurado de identificadores malformados retornando `400 Bad Request` via `MethodArgumentTypeMismatchException` en `GlobalExceptionHandler`. | `IncidentControllerV2Test.getIncidentByPublicId_whenMalformedUuid_shouldReturn400` | **PASS** |
| **RF-FRONTEND-V2** | Consumo integro de la API v2 desde los servicios Axios y adaptacion de componentes (ej. `AssignmentModal`) para operar con UUIDs sin errores de casteo. | `incidentService.test.js`, `triageService.test.js`, `notificationService.test.js`, `analyticsService.test.js`, `AssignmentModal.test.jsx` | **PASS** |

---

## 3. Metricas de Cobertura Tecnica de Codigo

### Backend — JaCoCo Report
- **Instrucciones Totales:** **78% de cobertura** (13.551 de 17.190 cubiertas).
- **Ramas Totales:** **62% de cobertura** (1.161 de 1.861 cubiertas).
- **Lineas Totales:** **80.7% de cobertura** (2.972 de 3.681 lineas cubiertas).
- **Clases analizadas:** 91 clases.
- **Paquetes clave:**
  - `org.bulbaseros.config`: **100% instrucciones**, **87% ramas**.
  - `org.bulbaseros.controller`: **96% instrucciones**, **70% ramas**.
  - `org.bulbaseros.dto`: **96% instrucciones**, **73% ramas**.
  - `org.bulbaseros.service.resilience`: **96% instrucciones**, **83% ramas**.
  - `org.bulbaseros.security`: **93% instrucciones**, **71% ramas**.
  - `org.bulbaseros.service`: **79% instrucciones**, **66% ramas**.

### Frontend — Vitest Coverage Report
- **Lineas (Lines):** **83.34%** (1.586 / 1.903 lineas).
- **Sentencias (Statements):** **82.21%** (1.641 / 1.996 sentencias).
- **Funciones (Functions):** **76.05%** (362 / 476 funciones).
- **Ramas (Branches):** **72.05%** (1.325 / 1.839 ramas).
- **Servicios migrados:**
  - `analyticsService.js`: **100% lineas**, **100% ramas**.
  - `triageService.js`: **96.36% lineas**, **100% ramas**.
  - `notificationService.js`: **100% lineas**, **50% ramas**.

---

## 4. Guia de Verificacion Manual

Para probar la refactorizacion de identificadores publicos y la API v2 de la forma mas simple posible:

1. **Iniciar la base de datos PostgreSQL:**
   ```bash
   docker compose up -d db
   ```
   *Verificar que `diagramUrbanPulse.sql` inicializa las tablas con `public_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid()` sin errores de extension ni dependencias.*

2. **Arrancar el Backend:**
   ```bash
   cd Backend
   mvn spring-boot:run
   ```

3. **Arrancar el Frontend:**
   ```bash
   cd frontend
   npm run dev
   ```

4. **Verificar la API v2 y la proteccion de IDs internos:**
   - Iniciar sesion con credenciales de prueba en `http://localhost:5173`.
   - Abrir la consola de red de las herramientas de desarrollo del navegador (DevTools -> Red / Network).
   - Crear una nueva incidencia desde el mapa o consultar el detalle de una incidencia existente:
     - Comprobar que la peticion HTTP se realiza a `/api/v2/incidents` o `/api/v2/incidents/{uuid}`.
     - Comprobar en la respuesta JSON que el campo `id` es un UUID v4 (ej. `3fa85f64-5717-4562-b3fc-2c963f66afa6`) y no un numero entero secuencial.
   - Enviar una peticion con un UUID malformado:
     ```bash
     curl -i -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v2/incidents/id-invalido
     ```
     *Verificar que el servidor responde `HTTP 400 Bad Request` estructurado en vez de un error 500.*
