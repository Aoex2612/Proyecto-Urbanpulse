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

## [ ] Tarea 5: Componente de Mapa Cartográfico con Leaflet y Modal de Creación - Depende de Tarea 4 - RF: RF-1, RF-2, RF-4, RF-5, RF-6, RF-7, RF-8, RF-9, RF-11, RF-12
- [ ] 5.1 Implementar componente `IncidentsMap.jsx` centrado por defecto en Málaga (`[36.7213, -4.4214]`, zoom 13), integrando capas OpenStreetMap y marcadores interactivos con Popup nativo que muestre título, descripción, categoría, estado y botón "Ver detalle completo".
- [ ] 5.2 Implementar botón toggle "Crear incidencia" / "Cancelar selección" y escucha de clics en el lienzo del mapa (`useMapEvents`) activa solo en modo creación; fuera de este modo, los clics no realizan acciones y cierran popups abiertos.
- [ ] 5.3 Implementar componente modal `CreateIncidentModal.jsx` con campos de título, descripción, selector desplegable de categoría predefinida (`Alumbrado`, `Vías Públicas`, `Limpieza`, `Parques y Jardines`, `Mobiliario Urbano`) y coordenadas capturadas, descartando el punto si se cancela.
- [ ] 5.4 Actualizar dinámicamente los marcadores del mapa al registrar una incidencia con éxito sin recargar la página.

## [ ] Tarea 6: Modal de Detalle de Incidencia y Componente Modular `Timeline` de Auditoría - Depende de Tarea 4 - RF: RF-2, RF-3, RF-15, RF-18, RF-19, RF-20
- [ ] 6.1 Implementar componente modular reutilizable `StatusTimeline.jsx` en `frontend/src/components/Timeline/` con JSDoc, renderizando cronológicamente autor, estados origen/destino, motivo justificado y fecha/hora.
- [ ] 6.2 Implementar modal de detalle `IncidentDetailModal.jsx` que se abra al pulsar "Ver detalle completo" desde el Popup del marcador, mostrando la información de la incidencia.
- [ ] 6.3 Renderizar condicionalmente `StatusTimeline` dentro de `IncidentDetailModal.jsx` exclusivamente cuando el usuario autenticado posea rol `ADMIN`, desacoplado para facilitar su futura extensión al rol `OPERATOR`.

## [ ] Tarea 7: Vista `/map`, Enrutamiento y Navegación en Frontend - Depende de Tareas 5 y 6 - RF: RF-1, RF-6, RF-7
- [ ] 7.1 Crear vista de página `MapPage.jsx` y su hoja de estilos `MapPage.css` orquestando el mapa a pantalla completa, barra superior con botón "Crear incidencia" / "Cancelar selección" y modales asociados.
- [ ] 7.2 Registrar la ruta protegida `/map` en `App.jsx` mediante `<ProtectedRoute>`.
- [ ] 7.3 Añadir enlace de navegación visible al "Mapa" en la cabecera/navegación accesible para usuarios autenticados.
- [ ] 7.4 Ejecutar y validar `npm run lint` en frontend garantizando 0 errores y advertencias.

## [ ] Tarea 8: Verificación Integral de Criterios de Aceptación y Pruebas - Depende de Tareas 3 y 7 - RF: Todos
- [ ] 8.1 Ejecutar suite completa de tests de Backend (`mvn clean test`) comprobando 100% de tests en verde.
- [ ] 8.2 Validar cada uno de los criterios de aceptación y casos límite especificados en `spec.md`.

---
## Mantenimiento (checklist recurrente)

- [ ] Verificar que no se introducen claves API privadas ni mapas de terceros de pago (mantener Leaflet + OpenStreetMap).
- [ ] Verificar que toda modificación de estado mantenga la inserción atómica en `status_changes`.
- [ ] Validar compatibilidad de `StatusTimeline` ante futuras vistas o paneles (ej. panel de operador).
