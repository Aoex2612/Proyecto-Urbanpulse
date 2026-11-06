# ADR-07: Control Centralizado del Ciclo de Vida y Asignación Multidepartamental en Capa de Servicio

* Estado: aceptado
* Fecha: 2026-11-05

## Contexto
El flujo de triaje operativo de incidencias de UrbanPulse (Feature 005, HU07–HU12) abarca transiciones de ciclo de vida con reglas estrictas de autorización, precondiciones de estado (`REPORTED` → `VALIDATED` / `REJECTED` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED` → `CLOSED` / `REOPENED`), asignaciones múltiples a departamentos y técnicos, y cálculo asistido de prioridad.
Se requería definir la arquitectura para gobernar estas transiciones y asignaciones garantizando integridad de datos, consistencia transaccional y respeto a las directrices de desacoplamiento de la `constitution/`.

## Opciones consideradas
* **Opción 1: Motor de flujos de trabajo / BPM externo (ej. Camunda o Spring StateMachine):** Integrar un framework completo de máquinas de estado con artefactos de configuración gráfica o listeners de eventos.
* **Opción 2: Gestión delegada en controladores REST con lógica dispersa:** Exponer múltiples endpoints HTTP específicos por cada acción (`/validate`, `/assign`, `/resolve`, `/close`) gestionando validaciones y consultas SQL directamente en cada controlador o repositorio.
* **Opción 3: Servicio de Dominio Centralizado (`IncidentTriageService` / `IncidentService`) con Guardas de Estado y DTOs específicos:** Orquestar de forma atómica (`@Transactional`) en la capa de servicios las comprobaciones de estado previo, verificación de roles JWT, reglas de correspondencia departamento-técnico y persistencia coordinada en `incidents`, `assignments`, `status_changes` e `incident_comments`.

## Decisión tomada
Optamos por la **Opción 3: Servicio de Dominio Centralizado con Guardas de Estado y Transaccionalidad Atómica**, complementado con componentes dedicados para el cálculo determinista de prioridad asistida y la gestión de comentarios.

### Razón / Por qué se tomó la decisión
* **Simplicidad y alineación con la arquitectura monolítica modular:** Evita la pesada sobrecarga cognitiva y operativa de un framework BPM externo (como Spring StateMachine), manteniendo el código legible, predecible y fácilmente testeable con JUnit 5 y Mockito.
* **Consistencia transaccional garantizada:** Permite que operaciones compuestas (como asignar departamentos, cambiar el estado a `ASSIGNED` y registrar la fila inmutable de auditoría en `status_changes`) ocurran dentro de una única transacción de base de datos (`@Transactional`), evitando estados inconsistentes si falla algún paso.
* **Desacoplamiento estricto:** La capa de presentación (`IncidentController`, `TriageController`, `CommentController`) solo maneja DTOs validados mediante Bean Validation (`@Valid`), mientras que la lógica de negocio y las transformaciones de entidades residen en servicios y mappers.

## Consecuencias
* **Positivo:** Alta cohesión y cobertura de pruebas unitarias sobre transiciones válidas e inválidas sin necesidad de arrancar contextos complejos; trazabilidad completa de cada transición.
* **Positivo:** Flexibilidad para soportar asignaciones simultáneas a varios departamentos y técnicos vinculando la tabla relacional `assignments` de forma limpia.
* **Negativo:** El servicio de triaje e incidencias concentra una cantidad notable de validaciones de reglas de negocio, por lo que debe mantenerse modularizado mediante helpers o servicios especializados (ej. servicio de priorización y servicio de asignaciones/comentarios).
* **Neutral:** El frontend debe coordinar el consumo de endpoints especializados reflejando en tiempo real las acciones permitidas según el rol y el estado actual de la incidencia.
