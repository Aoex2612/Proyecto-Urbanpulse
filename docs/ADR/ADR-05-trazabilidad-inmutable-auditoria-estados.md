# ADR-05: Trazabilidad inmutable mediante tabla relacional dedicada e inserción de transiciones de estado

* Estado: aceptado
* Fecha: 2026-10-26

## Contexto
El ciclo de vida de una incidencia urbana (`REPORTED` -> `VALIDATED` -> `ASSIGNED` -> `IN_PROGRESS` -> `RESOLVED` / `CLOSED` / `REJECTED`) involucra múltiples actores (ciudadanos, operadores, técnicos) y decisiones administrativas.
Se requería una solución que permitiese:
1. Reconstruir con precisión forense toda la historia de cambios de cada incidencia (quién ejecutó el cambio, cuándo ocurrió, estado previo, estado resultante y justificación o notas).
2. Proteger el histórico frente a modificaciones o borrados accidentales o intencionados.
3. Exponer un timeline visual y cronológico en el frontend tanto para el seguimiento ciudadano como para la auditoría municipal.

## Opciones consideradas
* **Opción 1: Auditoría genérica mediante Hibernate Envers o triggers de CDC (Change Data Capture):** Versionar automáticamente todas las tablas relacionales del sistema.
* **Opción 2: Almacenar un campo JSON / JSONB de eventos en la propia tabla `incidents`:** Agregar un array de transiciones dentro de una columna semiestructurada en la entidad principal.
* **Opción 3: Entidad y tabla relacional dedicada e inmutable (`status_changes` / `IncidentStatusHistory`):** Registrar cada transición de estado como una fila independiente de solo lectura vinculada a la incidencia mediante clave foránea.

## Decisión tomada
Optamos por la **Opción 3: Tabla relacional dedicada e inmutable (`status_changes`) con inserción de transiciones en el servicio de negocio**, vinculada mediante clave foránea a `incidents` y `users`.

### Razón / Por qué se tomó la decisión
* **Inmutabilidad y semántica de dominio explícita:** Cada fila representa un evento de negocio deliberado (`status_changes`), no un simple volcado técnico de columnas modificadas. Una vez insertado un cambio de estado, nunca se actualiza (`UPDATE`) ni se borra (`DELETE`).
* **Integridad referencial e interoperabilidad SQL:** Permite realizar consultas relacionales directas (`JOIN` con usuarios y departamentos), ordenación cronológica indexada y agregaciones estadísticas sobre tiempos de resolución entre estados.
* **Simplicidad frente a frameworks de auditoría genéricos:** Evita la sobrecarga de esquemas duplicados o metadatos crípticos que generan herramientas como Hibernate Envers, haciendo que los mappers hacia DTOs del timeline (`StatusChangeDTO`) sean directos y eficientes.
* **Portabilidad:** Funciona idénticamente en PostgreSQL y en la base de datos de pruebas H2 en memoria sin configuraciones propietarias.

## Consecuencias
* **Positivo:** Trazabilidad inmutable completa y transparente, soporte directo para el componente de línea temporal (`Timeline`) en el frontend, y facilidad para que analistas y operadores consulten métricas de tiempo de ciclo.
* **Negativo:** El volumen de la tabla `status_changes` crece linealmente con cada transición; requerirá estrategias de particionado o archivado a largo plazo si la plataforma gestiona millones de incidencias.
* **Neutral:** La responsabilidad de registrar el cambio de estado en la tabla de auditoría debe ser orquestada atómicamente (`@Transactional`) dentro del servicio (`IncidentService`) al modificar el estado de la incidencia.
