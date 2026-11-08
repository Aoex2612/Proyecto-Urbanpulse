# QA Note — Feature 007: Activos Urbanos y Contexto Externo (HU16-HU18)

**Fecha:** 08/11/2026  
**Rama:** `feature/007-activos-urbanos-y-contexto-externo`  
**Estado de Validación:** APROBADA (ÉXITO) ✅  
**Responsable QA:** Subagente de Validación SDD  

---

## 1. Resumen de la Validación y Veredicto

Se ha realizado la auditoría técnica y funcional completa de la **Feature 007** («Activos Urbanos y Contexto Externo»), correspondiente a las Historias de Usuario **HU16** (Asociación con Activo Urbano por proximidad geográfica), **HU17** (Ingesta de Contexto Externo: meteorología y tráfico de Málaga) y **HU18** (Degradación Controlada y Resiliencia mediante Circuit Breaker con timeout de 3.000 ms).

### Veredicto: **APROBADA CON ÉXITO (100% de cumplimiento)**
- **Requisitos Funcionales (RF-1 al RF-15):** 100% cubiertos y validados con tests automáticos.
- **Requisitos No Funcionales (RNF):** Tolerancia a fallos demostrada, ausencia de SPOF, control de accesos RBAC y compatibilidad desacoplada Frontend-Backend.
- **Linters:** Frontend ESLint sin errores (`0 errors, 0 warnings`).
- **Suites de Pruebas:**
  - **Frontend (Vitest):** 32 suites de test, **254 tests pasados con éxito** (0 fallos).
  - **Backend (Maven / JUnit 5):** **647 tests pasados con éxito** (0 errores, 0 fallos, 1 omitido condicionalmente `@EnabledIfPostgresAvailable`).

---

## 2. Tabla de Trazabilidad de Requisitos Funcionales (EARS) y Tests

| Requisito EARS | Descripción del Requisito | Suites / Tests que lo cubren | Resultado |
|---|---|---|:---:|
| **RF-1** | Búsqueda de activos dentro de un radio de proximidad de 50 metros al crear la incidencia | `UrbanAssetServiceTest.shouldAssociateNearbyAssetsSuccessfully` | **PASS** |
| **RF-2** | Cálculo de distancia geodésica con Haversine, cálculo de confianza probabilística ($1.0 - d/50$) y estado `is_confirmed = false` | `UrbanAssetServiceTest.shouldCalculateDistanceAndConfidenceCorrectly` | **PASS** |
| **RF-3** | Confirmación manual de activo (`is_confirmed = true`) vía `POST /api/incidents/{incidentId}/assets/{assetId}/confirm` por `OPERATOR`/`ADMIN` | `IncidentControllerSecurityTest.shouldAllowOperatorToConfirmAssetWith200`, `IncidentAssetCard.test.jsx` | **PASS** |
| **RF-4** | Desvinculación de activo vía `DELETE /api/incidents/{incidentId}/assets/{assetId}` por `OPERATOR`/`ADMIN` | `IncidentControllerSecurityTest.shouldAllowOperatorToDeleteAssetWith200`, `IncidentAssetCard.test.jsx` | **PASS** |
| **RF-5** | Inclusión de `urbanAssets` en `GET /api/incidents/{id}` y presentación con badges en React | `IncidentServiceTest.shouldGetIncidentByIdWithAssetsAndContext`, `IncidentAssetCard.test.jsx` | **PASS** |
| **RF-6** | Búsqueda sin activos candidatos en 50m guarda la incidencia con colección vacía sin lanzar error | `UrbanAssetServiceTest.shouldReturnEmptyWhenNoAssetsWithinRadius`, `IncidentAssetCard.test.jsx` | **PASS** |
| **RF-7** | Error estructurado HTTP 404 al intentar confirmar o eliminar un activo no asociado | `IncidentControllerSecurityTest.shouldReturn404WhenConfirmingNonExistingAssetAssociation` | **PASS** |
| **RF-8** | Ingesta automática de métricas meteorológicas vigentes (temperatura, precipitación, viento) con Open-Meteo | `ExternalContextServiceTest.shouldCaptureAndSaveWeatherContextSuccessfully` | **PASS** |
| **RF-9** | Persistencia relacional de lecturas en `external_observations` y vínculo en `incident_contexts` | `UrbanAssetsAndContextRepositoriesTest` | **PASS** |
| **RF-10** | Inclusión de `urbanContext` en `GET /api/incidents/{id}` y tarjeta reactiva en Frontend | `IncidentServiceTest.shouldGetIncidentByIdWithAssetsAndContext`, `IncidentContextCard.test.jsx` | **PASS** |
| **RF-11** | Registro de observación de tráfico municipal en el tramo de la incidencia | `ExternalContextServiceTest.shouldCaptureTrafficObservationSuccessfully` | **PASS** |
| **RF-12** | Timeout estricto de conexión y lectura configurado a 3.000 ms en el cliente HTTP | `ExternalContextServiceTest.shouldRespectTimeoutConfiguration` | **PASS** |
| **RF-13** | Activación de Fallback silencioso ante timeout, errores de red o códigos HTTP 5xx/4xx de APIs externas | `ExternalContextServiceTest.shouldActivateFallbackOnHttpErrorOrTimeout` | **PASS** |
| **RF-14** | Circuit Breaker con ventana móvil de 10 peticiones, umbral de 50% de fallos y enfriamiento de 30s | `CircuitBreakerTest` (transiciones CLOSED -> OPEN -> HALF_OPEN -> CLOSED) | **PASS** |
| **RF-15** | Alta de incidencias garantizada (`HTTP 201 Created`) con listas vacías en caso de caída total de fuentes externas | `IncidentServiceTest.shouldCreateIncidentWithFallbackWhenAssetOrContextFails` | **PASS** |

---

## 3. Métricas de Cobertura Técnica de Código

### Backend — JaCoCo Report
- **Instrucciones Totales:** **93% de cobertura** ($12.123$ de $12.934$ instrucciones cubiertas).
- **Ramas Totales:** **75% de cobertura** ($1.046$ de $1.379$ ramas cubiertas).
- **Paquetes clave de la Feature:**
  - `org.bulbaseros.service.resilience`: **96% instrucciones**, **83% ramas**.
  - `org.bulbaseros.mappers`: **98% instrucciones**, **90% ramas**.
  - `org.bulbaseros.controller`: **98% instrucciones**, **80% ramas**.
  - `org.bulbaseros.dto`: **96% instrucciones**, **73% ramas**.
  - `org.bulbaseros.service`: **92% instrucciones**, **74% ramas**.

### Frontend — Vitest Coverage Report
- **Archivos de la Feature:**
  - `IncidentAssetCard.jsx`: **100% líneas**, **84.8% ramas**, **100% funciones**, **100% sentencias**.
  - `IncidentContextCard.jsx`: **100% líneas**, **84.8% ramas**, **100% funciones**, **100% sentencias**.
  - `incidentService.js`: **100% líneas en nuevos métodos**, **71.4% ramas**.
  - `IncidentDetailModal.jsx`: **85.3% sentencias**, **84% líneas**, **67% ramas**.

---

## 4. Guía de Pruebas Manuales Simples

Para validar manualmente la feature en un entorno local, sigue estos sencillos pasos:

### Paso 1: Levantar servicios y frontend
1. En una terminal, ejecuta la base de datos y el backend:
   ```bash
   docker compose up -d
   cd Backend && mvn spring-boot:run
   ```
2. En otra terminal, levanta la interfaz de usuario:
   ```bash
   cd frontend && npm run dev
   ```

### Paso 2: Reporte de Incidencia con Activos Cercanos (HU16, HU17)
1. Inicia sesión como ciudadano: `ciudadano@urbanpulse.es` (o regístrate).
2. Pulsa en el mapa en una zona céntrica de Málaga (ej. Calle Marqués de Larios o Alameda Principal, coords ~ `36.7196, -4.4214`).
3. Completa el formulario de reporte y pulsa **Crear Incidencia**.
4. **Verificación:** La incidencia se crea de inmediato sin retraso alguno (`HTTP 201 Created`).

### Paso 3: Triaje de Activos Urbanos y Contexto (HU16, HU18)
1. Cierra sesión e inicia sesión como operador: `operador@urbanpulse.es` (password correspondiente en `database/data.sql`).
2. Entra en el panel de operador y abre la incidencia recién creada.
3. Observa la sección **Activos Urbanos Cercanos**:
   - Aparece el activo detectado (ej. semáforo, papelera o farola) con su código, distancia en metros (ej. `📍 14.2 m`), badge de confianza y etiqueta `Sugerido`.
   - Se muestran los botones `Confirmar` y `Desvincular`.
4. Pulsa **Confirmar**: la etiqueta cambia reactivamente a `✓ Confirmado`.
5. Observa la sección **Contexto Ambiental y de Tráfico**:
   - Se muestran las tarjetas con la Temperatura en °C, Precipitación en mm, Viento en km/h y estado del tráfico capturados al momento del reporte.
