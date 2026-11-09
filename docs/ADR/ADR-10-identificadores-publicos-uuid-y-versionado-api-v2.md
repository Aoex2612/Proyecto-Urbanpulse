# ADR-10: Adopcion de Identificadores Publicos UUID v4 y Versionado de API v2

* Estado: Propuesto
* Fecha: 2026-11-09

## Contexto
En la arquitectura actual de UrbanPulse, las entidades JPA (como `Incidencia`, `Usuario`, `Departamento`, `ActivoUrbano`, etc.) emplean identificadores numericos secuenciales generados por la base de datos (`BIGSERIAL` / `GenerationType.IDENTITY`). 

Estos IDs autonumericos se exponian directamente en las rutas REST (`/api/incidents/{id}`) y en los objetos de transferencia DTO (`IncidentResponse`, etc.). Esto plantea serios riesgos de seguridad:
1. **Ataques de enumeracion (IDOR):** Permite a atacantes o usuarios deducir el volumen total de incidencias, usuarios o activos, e iterar IDs de forma predecible.
2. **Filtracion de volumen y metadatos de negocio:** Permite inferir el ritmo de creacion y crecimiento de la plataforma.

Se requiere desacoplar el identificador interno de persistencia del identificador de dominio publico, y versionar la API para soportar la transicion limpia a v2 manteniendo la retrocompatibilidad con v1.

## Opciones consideradas
* **Opcion 1: Reemplazar claves primarias de BD por UUID v4 nativo.** Modificar las tablas para que la `PK` sea un `UUID`. Descartada porque degrada el rendimiento de indices B-Tree en inserciones masivas y complica las claves foraneas existentes.
* **Opcion 2: Esquema hibrido con Surrogate Key numerica interna + UUID v4 publico (`public_id`) con sustitucion en DTOs y Versionado API v2 (Elegida).** Mantener la PK `id BIGSERIAL` para indexacion interna de alto rendimiento y relaciones de base de datos, y anadir una columna `public_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid()` para la exposicion publica. Exponer endpoints `/api/v2/...` que operan con `public_id` y mantener `/api/v1/...` (y `/api/...` retrocompatible) para clientes legacy.
* **Opcion 3: Ofuscacion numerica (Hashids o Sqids).** Codificar el ID numerico en un string alfanumerico en la capa web. Descartada porque introduce acoplamiento criptografico o claves compartidas y puede ser revertida si la semilla se ve comprometida.

## Decision tomada
Optamos por la **Opcion 2**, aplicando un identificador publico basado en **UUID v4** en todas las entidades expuestas del sistema, y desplegando una estrategia de **versionado de endpoints (`/api/v1` y `/api/v2`)**:

1. **Modelo de datos y persistencia:**
   - Incorporacion del campo `public_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid()` en las tablas de `diagramUrbanPulse.sql` (`incidents`, `users`, `departments`, `urban_assets`, `attachments`, `incident_comments`, `notifications`, `assignments`, `status_changes`, `external_observations`).
   - Mapeo en las entidades JPA correspondientes con el atributo `private UUID publicId;` inicializado mediante `@PrePersist` (`UUID.randomUUID()`) si es nulo.
   - Creacion de metodos en repositorios Spring Data JPA para busqueda por clave publica: `findByPublicId(UUID publicId)` y metodos de existencia/eliminacion correspondientes.

2. **Capa DTO y Mappers (Estrategia Opcion A):**
   - En las respuestas publicas (DTOs), el campo `id` pasara a ser de tipo `String` (o `UUID`) conteniendo exclusivamente el valor de `publicId`. El `id` numerico interno de base de datos nunca se expone fuera del backend.

3. **Versionado de Endpoints REST:**
   - **`/api/v1/...` y `/api/...`:** Endpoints heredados que operan con IDs numericos (`Long`).
   - **`/api/v2/...`:** Nuevos endpoints modernos que operan exclusivamente con `UUID` como path variable y parametro de consulta.

4. **Frontend:**
   - Actualizacion de los servicios Axios (`incidentService`, `triageService`, `notificationService`, `analyticsService`) para consumir las rutas `/api/v2/...` y manejar el identificador publico de manera transparente.

### Razon / Por que se tomo la decision
* **Seguridad y proteccion contra enumeracion:** Los UUID v4 son no predecibles (128 bits de entropia), eliminando vectores de ataque IDOR.
* **Rendimiento optimo de persistencia:** Mantiene la eficiencia de las claves primarias enteras y claves foraneas en PostgreSQL.
* **Transicion controlada sin romper clientes legacy:** El versionado explora rutas limpias `/api/v2` sin riesgo de romper integraciones previas.

## Consecuencias
* **Positivo:** Maxima seguridad en la exposicion de recursos a traves de la API web; el frontend y clientes externos no tienen visibilidad de la base de datos interna; compatibilidad hacia atras garantizada.
* **Compromiso / Esfuerzo:** Requiere mantener consultas duales en repositorios (`findById` y `findByPublicId`), mapeo cuidadoso en los DTOs y actualizacion coordinada de servicios y tests en Frontend y Backend.
