# 003 — Mapa de Incidencias con Leaflet y Auditoría Transversal — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [x] Tarea 1: Entidades, DTOs y Repositorios en Backend (Persistencia y Auditoría) - RF: RF-1, RF-10, RF-13, RF-15
- [x] 1.1 Verificar y completar los mapeos JPA de `Incidencia.java` y `CambiosEstado.java` con las tablas `incidents` y `status_changes`, asegurando tipos, relaciones `@ManyToOne` y anotaciones con Javadoc.
- [x] 1.2 Crear interfaz `IncidenciaRepository` (`JpaRepository<Incidencia, Long>`) en `org.bulbaseros.repository` con consulta para recuperar incidencias cuyo estado no esté en la lista de estados excluidos (`findByStatusNotIn(...)`).
- [x] 1.3 Crear interfaz `CambiosEstadoRepository` (`JpaRepository<CambiosEstado, Long>`) en `org.bulbaseros.repository` con el método de consulta `findByIncidentIdOrderByCreatedAtAsc(Long incidentId)`.
- [x] 1.4 Crear DTOs `CreateIncidentRequest` (con validaciones Jakarta `@NotBlank` para título, descripción y categoría de catálogo, y `@NotNull` para latitud y longitud), `IncidentResponse` y `StatusChangeResponse` documentados con Javadoc.

## [x] Tarea 2: Lógica de Negocio Transaccional en `IncidentService` - Depende de Tarea 1 - RF: RF-1, RF-10, RF-13, RF-14, RF-15, RF-16
- [x] 2.1 Implementar método `getActiveIncidents()` que consulte el repositorio excluyendo estados `CLOSED` y `REJECTED`, transformando a lista de `IncidentResponse`.
- [x] 2.2 Implementar método `createIncident(CreateIncidentRequest request, String userEmail)` que asocie el ciudadano emisor mediante búsqueda por email del token JWT, inicialice estado `REPORTED`, prioridad `MEDIUM`, persista y retorne `IncidentResponse`.
- [x] 2.3 Implementar método transaccional `@Transactional updateIncidentStatus(Long id, UpdateIncidentStatusRequest request, String userEmail)` que valide existencia (404), actualice el estado de `Incidencia` e inserte la transición en `CambiosEstado` con estado previo, nuevo estado, motivo y usuario actor.
- [x] 2.4 Implementar método `getIncidentHistory(Long id)` que verifique la existencia de la incidencia (lanzando 404 si no existe) y devuelva la lista cronológica de transiciones como `StatusChangeResponse`.
- [x] 2.5 Crear pruebas unitarias con Mockito en `IncidentServiceTest` para verificar filtrado de activas, creación con categoría válida, auditoría transaccional de estados, captura de 404 e historial ordenado.

## [x] Tarea 3: Endpoints REST y Reglas de Seguridad RBAC en Backend - Depende de Tarea 2 - RF: RF-1, RF-10, RF-13, RF-14, RF-15, RF-16, RF-17
- [x] 3.1 Actualizar `IncidentController` inyectando `IncidentService` y exponiendo `GET /api/incidents`, `POST /api/incidents` (HTTP 201), `PATCH /api/incidents/{id}/status` y `GET /api/incidents/{id}/history` (HTTP 200 / 404) documentados con Javadoc.
- [x] 3.2 Actualizar `SecurityConfig` declarando las reglas de autorización: `GET /api/incidents/{id}/history` restringido a `.hasRole("ADMIN")`, `PATCH /api/incidents/*/status` a `.hasAnyRole("OPERATOR", "ADMIN")`, y endpoints generales a `.authenticated()`.
- [x] 3.3 Desarrollar suite de pruebas de integración y seguridad en `IncidentControllerSecurityTest` comprobando respuestas 200, 201, 401, 403 y 404 según roles (`ANONYMOUS`, `CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ANALYST`, `ADMIN`).

## [x] Tarea 4: Dependencias Cartográficas y Capa de Servicio en Frontend - RF: RF-1, RF-10, RF-15
- [x] 4.1 Instalar e incorporar dependencias `leaflet` y `react-leaflet` en `frontend/package.json` e importar `leaflet/dist/leaflet.css`.
- [x] 4.2 Configurar la resolución correcta de iconos y assets de marcadores de Leaflet para el entorno de empaquetado de Vite.
- [x] 4.3 Crear módulo `incidentService.js` en `frontend/src/services/` con métodos `getAllIncidents()`, `getIncidentById(id)`, `createIncident(data)`, `updateStatus(id, data)` y `getIncidentHistory(id)` con JSDoc.

## [x] Tarea 5: Componente de Mapa Cartográfico con Leaflet y Modal de Creación - Depende de Tarea 4 - RF: RF-1, RF-2, RF-4, RF-5, RF-6, RF-7, RF-8, RF-9, RF-11, RF-12
- [x] 5.1 Implementar componente `IncidentsMap.jsx` centrado por defecto en Málaga (`[36.7213, -4.4214]`, zoom 13), integrando capas OpenStreetMap y marcadores interactivos con Popup nativo que muestre título, descripción, categoría, estado y botón "Ver detalle completo".
- [x] 5.2 Implementar botón toggle "Crear incidencia" / "Cancelar selección" y escucha de clics en el lienzo del mapa (`useMapEvents`) activa solo en modo creación; fuera de este modo, los clics no realizan acciones y cierran popups abiertos.
- [x] 5.3 Implementar componente modal `CreateIncidentModal.jsx` con campos de título, descripción, selector desplegable de categoría predefinida (`Alumbrado`, `Vías Públicas`, `Limpieza`, `Parques y Jardines`, `Mobiliario Urbano`) y coordenadas capturadas, descartando el punto si se cancela.
- [x] 5.4 Actualizar dinámicamente los marcadores del mapa al registrar una incidencia con éxito sin recargar la página.

## [x] Tarea 6: Modal de Detalle de Incidencia y Componente Modular `Timeline` de Auditoría - Depende de Tarea 4 - RF: RF-2, RF-3, RF-15, RF-18, RF-19, RF-20
- [x] 6.1 Implementar componente modular reutilizable `StatusTimeline.jsx` en `frontend/src/components/Timeline/` con JSDoc, renderizando cronológicamente autor, estados origen/destino, motivo justificado y fecha/hora.
- [x] 6.2 Implementar modal de detalle `IncidentDetailModal.jsx` que se abra al pulsar "Ver detalle completo" desde el Popup del marcador, mostrando la información de la incidencia.
- [x] 6.3 Renderizar condicionalmente `StatusTimeline` dentro de `IncidentDetailModal.jsx` exclusivamente cuando el usuario autenticado posea rol `ADMIN`, desacoplado para facilitar su futura extensión al rol `OPERATOR`.

## [x] Tarea 7: Vista `/map`, Enrutamiento y Navegación en Frontend - Depende de Tareas 5 y 6 - RF: RF-1, RF-6, RF-7
- [x] 7.1 Crear vista de página `MapPage.jsx` y su hoja de estilos `MapPage.css` orquestando el mapa a pantalla completa, barra superior con botón "Crear incidencia" / "Cancelar selección" y modales asociados.
- [x] 7.2 Registrar la ruta protegida `/map` en `App.jsx` mediante `<ProtectedRoute>`.
- [x] 7.3 Añadir enlace de navegación visible al "Mapa" en la cabecera/navegación accesible para usuarios autenticados.
- [x] 7.4 Ejecutar y validar `npm run lint` en frontend garantizando 0 errores y advertencias.

## [x] Tarea 8: Verificación Integral de Criterios de Aceptación y Pruebas - Depende de Tareas 3 y 7 - RF: Todos
- [x] 8.1 Ejecutar suite completa de tests de Backend (`mvn clean test`) comprobando 100% de tests en verde.
- [x] 8.2 Validar cada uno de los criterios de aceptación y casos límite especificados en `spec.md`.

## [x] Tarea 9: Integración del Panel de Operador y Máquina de Estados del Ciclo de Vida - RF: RF-13, RF-14, RF-15
- [x] 9.1 Conectar `OperatorPanelPage.jsx` a `incidentService.getAllIncidents()` para sincronizar en tiempo real las incidencias de la base de datos PostgreSQL.
- [x] 9.2 Autorizar en `SecurityConfig` y `JwtAuthenticationFilter` el método HTTP `PATCH` y las peticiones preflight CORS `OPTIONS /**`.
- [x] 9.3 Implementar en backend (`IncidentService`) y frontend (`mapConstants.js`) la validación estricta de transiciones según el diagrama de ciclo de vida (`REPORTED` -> `VALIDATED`/`REJECTED`, `VALIDATED` -> `ASSIGNED`, `ASSIGNED` -> `IN_PROGRESS`, `IN_PROGRESS` -> `RESOLVED`, `RESOLVED` -> `CLOSED`/`REOPENED`, `REOPENED` -> `ASSIGNED`).
- [x] 9.4 Restringir en el selector del panel de operador únicamente los estados siguientes permitidos e inhabilitar transiciones en estados finales (`REJECTED`, `CLOSED`).
- [x] 9.5 Depuración y simplificación visual de la interfaz del panel de triaje de operador.

## [x] Tarea 10: Módulo utilitario de geolocalización y pruebas unitarias (HU14.4) - RF: RF-22, RF-26, RF-29, RF-30, RF-31, RF-33
- [x] 10.1 Crear `frontend/src/utils/geolocationUtils.js` con funciones `isGeolocationSupported()` y `getCurrentCoordinates(options)` basadas en Promesas, configurando `enableHighAccuracy: true`, `timeout: 10000` y `maximumAge: 0`.
- [x] 10.2 Implementar en `geolocationUtils.js` el formateo de coordenadas a 6 decimales y el mapeo de errores nativos (`PERMISSION_DENIED`, `POSITION_UNAVAILABLE`, `TIMEOUT`) a mensajes explicativos claros para el ciudadano, documentando con JSDoc.
- [x] 10.3 Desarrollar suite de pruebas unitarias en `frontend/src/utils/geolocationUtils.test.js` validando detección de soporte, resolución exitosa con 6 decimales y cobertura de todos los códigos de error.

## [x] Tarea 11: Diseño visual y estilos CSS del control de geolocalización (HU14.4) - Depende de Tarea 10 - RF: RF-21, RF-23, RF-24, RF-27, RF-29, RF-33
- [x] 11.1 Añadir estilos en `frontend/src/components/Map/Map.css` para el botón `.btn-current-location` con icono SVG de geoposicionamiento, estados `:hover`, `:focus-visible`, `:disabled` y animación de giro `.is-locating`.
- [x] 11.2 Diseñar clases para el distintivo de confirmación `.geo-status-badge` en verde esmeralda y la alerta de error en línea `.geo-error-banner` con semántica accesible y contraste WCAG 2.1 AA.

## [ ] Tarea 12: Integración del botón y máquina de estados en el modal de alta (HU14.4) - Depende de Tareas 10 y 11 - RF: RF-21 a RF-33, RF-35
- [ ] 12.1 Declarar estados en `CreateIncidentModal.jsx` (`isLocating`, `geoError`, `isLocationOverridden`, `isGeoSupported`, `currentCoords`) y salvaguarda contra desmontado asíncrono con `isMountedRef`.
- [ ] 12.2 Implementar manejador `handleUseCurrentLocation()` para gestionar la llamada asíncrona a `getCurrentCoordinates()`, actualizar inputs de latitud y longitud a 6 decimales ante éxito, o mostrar error contextual preservando coordenadas previas ante fallo.
- [ ] 12.3 Renderizar el botón "Usar mi ubicación actual" dentro del bloque de coordenadas, conmutando a "Obteniendo ubicación..." durante la carga y mostrando el badge "Ubicación actual obtenida" tras éxito.
- [ ] 12.4 Deshabilitar el botón de envío principal del formulario MIENTRAS `isLocating` esté activo para impedir registros intermedios.
- [ ] 12.5 Gestionar estado inhabilitado con mensaje explicativo si `isGeoSupported` es falso.

## [ ] Tarea 13: Sincronización cartográfica y centrado suave tras creación (HU14.4) - Depende de Tarea 12 - RF: RF-34, RF-35
- [ ] 13.1 Actualizar `MapPage.jsx` para pasar las coordenadas de la nueva incidencia como foco cartográfico en `handleIncidentCreated`.
- [ ] 13.2 Incorporar en `IncidentsMap.jsx` efecto o controlador de Leaflet para centrar suavemente (`flyTo` a zoom 16) la cámara del mapa sobre las coordenadas del nuevo reporte tras guardarse con éxito.
- [ ] 13.3 Asegurar descarte limpio de las coordenadas capturadas sin alterar el mapa cuando el usuario cancela o cierra el modal.

## [ ] Tarea 14: Pruebas unitarias de componente y validación global (HU14.4) - Depende de Tareas 12 y 13 - RF: RF-21 a RF-35
- [ ] 14.1 Ampliar `frontend/src/components/Map/CreateIncidentModal.test.jsx` con tests unitarios simulando: presencia del botón, transición a estado cargando con textos, actualización de lat/lng a 6 decimales, preservación de coordenadas ante error de permisos, y botón deshabilitado en navegadores incompatibles.
- [ ] 14.2 Ejecutar `npm test` verificando que todas las pruebas unitarias pasen en verde.
- [ ] 14.3 Ejecutar `npm run lint` en el frontend asegurando 0 errores de estilo y formato.
- [ ] 14.4 Validar contra todos los criterios de aceptación de `spec.md`.

---
## Mantenimiento (checklist recurrente)

- [ ] Verificar que no se introducen claves API privadas ni mapas de terceros de pago (mantener Leaflet + OpenStreetMap).
- [ ] Verificar que toda modificación de estado mantenga la inserción atómica en `status_changes`.
- [ ] Validar compatibilidad de `StatusTimeline` ante futuras vistas o paneles (ej. panel de operador).
