# 003 — Mapa de Incidencias con Leaflet y Auditoría Transversal — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [ ] Tarea 1: Entidades, DTOs y Repositorios en Backend (Persistencia y Auditoría) - RF: RF-6, RF-9, RF-11
- [ ] 1.1 Verificar y completar los mapeos JPA de `Incidencia.java` y `CambiosEstado.java` con las tablas `incidents` y `status_changes`, asegurando tipos, relaciones `@ManyToOne` y anotaciones con Javadoc.
- [ ] 1.2 Crear interfaz `IncidenciaRepository` (`JpaRepository<Incidencia, Long>`) en `org.bulbaseros.repository` para operaciones de consulta y persistencia de incidencias.
- [ ] 1.3 Crear interfaz `CambiosEstadoRepository` (`JpaRepository<CambiosEstado, Long>`) en `org.bulbaseros.repository` con el método de consulta `findByIncidentIdOrderByCreatedAtAsc(Long incidentId)`.
- [ ] 1.4 Crear DTOs `CreateIncidentRequest` (con validaciones Jakarta `@NotBlank` para título, descripción y categoría, y `@NotNull` para latitud y longitud), `IncidentResponse` y `StatusChangeResponse` documentados con Javadoc.

## [ ] Tarea 2: Lógica de Negocio Transaccional en `IncidentService` - Depende de Tarea 1 - RF: RF-1, RF-6, RF-9, RF-10, RF-11
- [ ] 2.1 Implementar método `getAllIncidents()` que consulte el repositorio y transforme las entidades a una lista de `IncidentResponse`.
- [ ] 2.2 Implementar método `createIncident(CreateIncidentRequest request, String userEmail)` que asocie el ciudadano a partir de su email del JWT, inicialice estado `REPORTED`, prioridad `MEDIUM`, persista y retorne `IncidentResponse`.
- [ ] 2.3 Implementar método transaccional `@Transactional updateIncidentStatus(Long id, UpdateIncidentStatusRequest request, String userEmail)` que actualice el estado de `Incidencia` e inserte la transición en `CambiosEstado` registrando estado previo, nuevo estado, motivo y usuario actor.
- [ ] 2.4 Implementar método `getIncidentHistory(Long id)` que verifique la existencia de la incidencia y devuelva la lista cronológica de transiciones como `StatusChangeResponse`.
- [ ] 2.5 Crear pruebas unitarias con Mockito en `IncidentServiceTest` para verificar creación de incidencia, auditoría transaccional de estados, captura de 404 e historial ordenado.

## [ ] Tarea 3: Endpoints REST y Reglas de Seguridad RBAC en Backend - Depende de Tarea 2 - RF: RF-1, RF-6, RF-9, RF-10, RF-11, RF-12
- [ ] 3.1 Actualizar `IncidentController` inyectando `IncidentService` y exponiendo `GET /api/incidents`, `POST /api/incidents` (HTTP 201), `PATCH /api/incidents/{id}/status` y `GET /api/incidents/{id}/history` documentados con Javadoc.
- [ ] 3.2 Actualizar `SecurityConfig` declarando las reglas de autorización: `GET /api/incidents/{id}/history` restringido a `.hasRole("ADMIN")`, `PATCH /api/incidents/*/status` a `.hasAnyRole("OPERATOR", "ADMIN")`, y endpoints generales a `.authenticated()`.
- [ ] 3.3 Desarrollar suite de pruebas de integración y seguridad en `IncidentControllerSecurityTest` comprobando respuestas 200, 201, 401, 403 y 404 según roles (`ANONYMOUS`, `CITIZEN`, `OPERATOR`, `ADMIN`).

## [ ] Tarea 4: Dependencias Cartográficas y Capa de Servicio en Frontend - RF: RF-1, RF-6, RF-11
- [ ] 4.1 Instalar e incorporar dependencias `leaflet` y `react-leaflet` en `frontend/package.json` e importar `leaflet/dist/leaflet.css`.
- [ ] 4.2 Configurar la resolución correcta de iconos y assets de marcadores de Leaflet para el entorno de empaquetado de Vite.
- [ ] 4.3 Crear módulo `incidentService.js` en `frontend/src/services/` con métodos `getAllIncidents()`, `createIncident(data)`, `updateStatus(id, data)` y `getIncidentHistory(id)` con JSDoc.

## [ ] Tarea 5: Componente de Mapa Cartográfico con Leaflet y Modal de Creación - Depende de Tarea 4 - RF: RF-1, RF-2, RF-3, RF-4, RF-5, RF-7, RF-8
- [ ] 5.1 Implementar componente `IncidentsMap.jsx` centrado por defecto en Málaga (`[36.7213, -4.4214]`, zoom 13), integrando capas OpenStreetMap y marcadores interactivos con tooltip/popup para incidencias existentes.
- [ ] 5.2 Implementar escucha de clics en el lienzo del mapa (`useMapEvents`) cuando el modo "Crear incidencia" esté activado para capturar latitud y longitud.
- [ ] 5.3 Implementar componente modal `CreateIncidentModal.jsx` con campos de título, descripción, categoría y coordenadas capturadas fijadas, validaciones y feedback de guardado.
- [ ] 5.4 Actualizar dinámicamente los marcadores del mapa al registrar una incidencia con éxito sin recargar la página.

## [ ] Tarea 6: Modal de Detalle de Incidencia y Componente Modular `Timeline` de Auditoría - Depende de Tarea 4 - RF: RF-2, RF-11, RF-13, RF-14, RF-15
- [ ] 6.1 Implementar componente modular reutilizable `StatusTimeline.jsx` en `frontend/src/components/Timeline/` con JSDoc, renderizando cronológicamente autor, estados origen/destino, motivo justificado y fecha/hora.
- [ ] 6.2 Implementar modal de detalle `IncidentDetailModal.jsx` mostrando la información pública de la incidencia seleccionada en el mapa para cualquier usuario autenticado.
- [ ] 6.3 Renderizar condicionalmente `StatusTimeline` dentro de `IncidentDetailModal.jsx` exclusivamente cuando el usuario autenticado posea rol `ADMIN`, desacoplado para facilitar su extensión al rol `OPERATOR`.

## [ ] Tarea 7: Vista `/map`, Enrutamiento y Navegación en Frontend - Depende de Tareas 5 y 6 - RF: RF-1, RF-4
- [ ] 7.1 Crear vista de página `MapPage.jsx` y su hoja de estilos `MapPage.css` orquestando el mapa a pantalla completa, barra de herramientas con botón "Crear incidencia" / cancelar y modales asociados.
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
