# 004 — Reporte y Seguimiento de Incidencias con Detección Territorial — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [x] Tarea 1: DTOs, Repositorio Paginado y Mapeadores en Backend - RF: RF-4, RF-12, RF-13, RF-14
- [x] 1.1 Crear DTO `ReverseGeocodeResponse` con campos `address`, `district` y `neighborhood` documentado con Javadoc.
- [x] 1.2 Crear DTO `PageResponse<T>` con campos `content`, `totalElements`, `totalPages`, `page` y `size` conforme al contrato de `docs/api.md`.
- [x] 1.3 Ampliar `IncidenciaRepository` con métodos de consulta paginados `Page<Incidencia> findByCitizenId(Long citizenId, Pageable pageable)` y `Page<Incidencia> findByCitizenIdAndStatus(Long citizenId, EstadoIncidencia status, Pageable pageable)`.
- [x] 1.4 Actualizar `IncidentMapper` para añadir método de conversión a `PageResponse<IncidentResponse>`.
- [x] 1.5 Crear pruebas `@DataJpaTest` en `IncidenciaRepositoryTest` verificando ordenación descendente por fecha, paginación y filtrado por estado.

## [x] Tarea 2: Servicio de Integración con Datos Abiertos de Málaga y Degradación Controlada en Backend - RF: RF-2, RF-6, RF-7, RF-8, RF-10, RF-11 - RNF: Tolerancia a fallos, timeout 3s
- [x] 2.1 Crear servicio `MalagaOpenDataService` en `org.bulbaseros.service` configurando cliente HTTP con `ConnectTimeout` y `ReadTimeout` de 3000 ms.
- [x] 2.2 Implementar método de resolución territorial consultando los recursos del callejero y delimitaciones oficiales del portal de Datos Abiertos del Ayuntamiento de Málaga.
- [x] 2.3 Implementar degradación controlada: ante excepciones de timeout, fallos de red o ausencia de correspondencia espacial, capturar el error y devolver campos nulos (`null`).
- [x] 2.4 Crear pruebas unitarias en `MalagaOpenDataServiceTest` con simulación de respuesta válida, respuesta sin coincidencias, timeout y error HTTP 500.

## [x] Tarea 3: Lógica de Negocio de Creación Enriquecida y Consulta Paginada en `IncidentService` - Depende de Tarea 1, Tarea 2 - RF: RF-1, RF-4, RF-5, RF-6, RF-7, RF-8, RF-10, RF-12, RF-13, RF-14, RF-15, RF-18
- [x] 3.1 Actualizar método `createIncident` en `IncidentService` para validar que las coordenadas pertenezcan al término municipal de Málaga (`[36.5000, 36.9000]`, `[-4.7000, -4.2000]`), arrojando excepción 400 si están fuera de rango.
- [x] 3.2 Integrar `MalagaOpenDataService` en `createIncident` para autocompletar dirección, barrio y distrito automáticamente si no vienen prefijados.
- [x] 3.3 Implementar método `getMyIncidents(String userEmail, EstadoIncidencia status, Pageable pageable)` que consulte las incidencias del ciudadano autenticado ordenadas descendentemente por fecha de creación.
- [x] 3.4 Implementar método `reverseGeocode(Double latitude, Double longitude)` delegando en `MalagaOpenDataService` con validación previa de coordenadas.
- [x] 3.5 Crear pruebas unitarias con Mockito en `IncidentServiceTest` verificando la creación con asignación territorial, degradación a null, validación de coordenadas y aislamiento por ciudadano.

## [ ] Tarea 4: Endpoints REST y Seguridad Perimetral en Backend - Depende de Tarea 3 - RF: RF-1, RF-2, RF-5, RF-12, RF-14, RF-15, RF-16, RF-18
- [ ] 4.1 Exponer en `IncidentController` el endpoint `GET /api/incidents/my-incidents` aceptando query params opcionales `status`, `page` y `size`, retornando `ResponseEntity<PageResponse<IncidentResponse>>`.
- [ ] 4.2 Exponer en `IncidentController` el endpoint `GET /api/incidents/geocode/reverse` con parámetros `latitude` y `longitude`, retornando `ResponseEntity<ReverseGeocodeResponse>`.
- [ ] 4.3 Actualizar `SecurityConfig` garantizando que `/api/incidents/my-incidents` y `/api/incidents/geocode/reverse` requieran autenticación (`.authenticated()`).
- [ ] 4.4 Crear pruebas de integración en `IncidentControllerTest` comprobando respuestas HTTP 200 con paginación, 400 ante coordenadas fuera de límites y 401 sin autenticación.

## [ ] Tarea 5: Capa de Servicio y Formulario de Alta con Solo Lectura en Frontend - Depende de Tarea 4 - RF: RF-1, RF-2, RF-3, RF-5, RF-9, RF-11
- [ ] 5.1 Incorporar en `frontend/src/services/incidentService.js` los métodos `getMyIncidents(params)` y `reverseGeocode(latitude, longitude)` documentados con JSDoc.
- [ ] 5.2 Actualizar `CreateIncidentModal.jsx` para invocar automáticamente `reverseGeocode` al recibir las coordenadas del mapa.
- [ ] 5.3 Renderizar en `CreateIncidentModal.jsx` los campos de Dirección, Barrio y Distrito en modo estrictamente de solo lectura, mostrando `"Sin asignar"` si son nulos.
- [ ] 5.4 Crear pruebas unitarias con Vitest en `CreateIncidentModal.test.jsx` comprobando la previsualización de la dirección, campos de solo lectura y descarte de coordenadas al cancelar.

## [ ] Tarea 6: Vista de Seguimiento "Mis Incidencias" y Navegación en Frontend - Depende de Tarea 5 - RF: RF-11, RF-12, RF-13, RF-14, RF-15, RF-16, RF-17
- [ ] 6.1 Crear componente `MyIncidentsPage.jsx` y hoja de estilos `MyIncidentsPage.css` con listado paginado (10 avisos por página), orden cronológico descendente y selector de filtro por estado (`TODOS` y estados del ciclo de vida).
- [ ] 6.2 Integrar en cada tarjeta o fila de `MyIncidentsPage` el botón "Ver detalle" para abrir `IncidentDetailModal` con la información completa y fechas clave.
- [ ] 6.3 Implementar vista de estado vacío descriptiva ("No tienes incidencias registradas") con botón de acción directa para navegar al mapa `/map`.
- [ ] 6.4 Registrar la ruta protegida `/my-incidents` en `App.jsx` y agregar el enlace de navegación en la barra superior para usuarios autenticados.
- [ ] 6.5 Crear pruebas unitarias con Vitest en `MyIncidentsPage.test.jsx` comprobando renderizado de avisos, cambio de página, filtrado por estado y estado vacío.

## [ ] Tarea 7: Verificación Integral de Criterios de Aceptación, Cobertura y Calidad - Depende de Tareas 4 y 6 - RF: Todos
- [ ] 7.1 Ejecutar suite completa de tests de Backend (`cd Backend && mvn clean test`) verificando 100% de tests en verde y generación de reporte JaCoCo.
- [ ] 7.2 Ejecutar suite de pruebas de Frontend (`cd frontend && npm test`) y análisis estático (`npm run lint`) garantizando 0 errores y advertencias.
- [ ] 7.3 Validar uno a uno los criterios de aceptación de `spec.md` asegurando que todos los comportamientos observables funcionen según especificación.

---
## Mantenimiento (checklist recurrente)

- [ ] Verificar que ninguna llamada externa a la API de datos abiertos exceda el timeout configurado de 3 segundos.
- [ ] Asegurar que el filtro `GET /api/incidents/my-incidents` siempre obtenga el identificador del usuario del token JWT y nunca de parámetros de petición manipulables.
- [ ] Validar que los campos territoriales mantengan el fallback a `"Sin asignar"` ante respuestas nulas.
