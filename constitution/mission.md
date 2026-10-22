# Misión — UrbanPulse

_Define la razón de ser del proyecto. Es la referencia que decide si una feature "encaja" o no._

## Qué construimos

UrbanPulse es una plataforma centralizada para la gestión, monitorización y resolución de incidencias urbanas en tiempo real. Permite centralizar el reporte ciudadano geolocalizado, la coordinación interdepartamental, el inventario de activos de infraestructura municipal y la auditoría completa del ciclo de vida de cada incidencia en la ciudad.

Piezas principales:
1. **Portal Ciudadano (Frontend React):** Permite a los ciudadanos registrarse, autenticarse, reportar incidencias geolocalizadas con contenido multimedia y consultar su estado de resolución.
2. **Servicio Backend (API REST en Java/Spring Boot):** Gestión de usuarios, autenticación JWT, lógica de negocio de incidencias y activos, validación y control de acceso basado en roles (RBAC).
3. **Persistencia y Auditoría (PostgreSQL):** Registro relacional de datos urbanos con historial inmutable de transiciones de estado, asociaciones a activos municipales y asignaciones departamentales.
4. **Catálogo y Monitorización de Activos Urbanos:** Identificación e inventario de infraestructura y mobiliario urbano (luminarias, papeleras, semáforos, etc.) vinculables a incidencias para optimizar las rutas y tareas de mantenimiento.

## Para quién

- **Ciudadanos (`CITIZEN`):** Reportan problemas en la vía pública, adjuntan evidencias y dan seguimiento a sus avisos.
- **Operadores municipales (`OPERATOR` / `ADMIN`):** Validan, priorizan y asignan incidencias al departamento o técnico correspondiente, coordinando la atención a activos afectados.
- **Técnicos de mantenimiento (`TECHNICIAN`):** Reciben asignaciones, gestionan la resolución sobre el terreno e informan intervenciones sobre activos o infraestructura.
- **Analistas urbanos (`ANALYST`):** Estudian métricas, tiempos de respuesta, degradación de activos y patrones espaciales de incidencias.

## Principios

- **Trazabilidad inmutable:** Todo cambio de estado de una incidencia queda registrado de forma auditable (quién, cuándo, estado anterior, estado nuevo y motivo).
- **Geolocalización como eje central:** Toda incidencia y activo municipal cuenta con coordenadas precisas (latitud y longitud) para su correcta atención física.
- **Vinculación con activos de infraestructura:** Facilidad para asociar incidencias a activos urbanos específicos (`urban_assets`), permitiendo trazabilidad del ciclo de vida y desgaste del equipamiento municipal.
- **Seguridad y desacoplamiento:** Separación estricta entre capa cliente y API REST, autenticación vía tokens JWT y control de acceso estricto según roles (RBAC).
- **Validación rigurosa en origen:** Las entradas deben validarse tanto en frontend (UX inmediata) como en backend (integridad y seguridad).

## Qué NO es

- **No es un servicio de emergencias críticas (112):** No sustituye a los canales de atención inmediata de policía, bomberos o urgencias médicas.
- **No es una red social:** La interacción está acotada al reporte, seguimiento oficial y notas de servicio; no fomenta foros abiertos ni discusiones no moderadas.
