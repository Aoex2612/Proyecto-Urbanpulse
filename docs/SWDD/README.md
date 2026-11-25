# Software Design Documents (DD) de UrbanPulse

Este directorio reúne los **Software Design Documents** (también llamados *Design Docs* o *TDD*) de UrbanPulse. Su finalidad es explicitar **cómo se diseñó e implementó** cada cambio relevante, **qué decisiones técnicas se tomaron** y **por qué se eligieron frente a otras alternativas**, siguiendo la plantilla [`SOFTWARE_DESIGN_DOCUMENT_TEMPLATE.md`](../../.github/SOFTWARE_DESIGN_DOCUMENT_TEMPLATE.md) y las pautas de [`DocumentosDiseñoSoftware.pdf`](https://www.uma.es/) (documento de referencia de la asignatura *Desarrollo Software en Plataforma en la Nube*).

> **No confundir:** los `spec.md` / `plan.md` / `tasks.md` de [`features/`](../../features/) son documentación del flujo **SDD = Spec-Driven Development**. Los documentos de este directorio son **SDD = Software Design Document** y se identifican con el prefijo `DD-`.

## Relación con los ADR

Siguiendo el apartado 7 del material de referencia, todo Design Doc incluye una sección **«Decisiones arquitectónicas derivadas»** con enlaces a los ADR que lo respaldan, y todo ADR incorpora un enlace inverso al Design Doc que lo origina. La relación es, por tanto, bidireccional.

La serie de ADR se ha normalizado para que cada decisión tenga un identificador único, coherente entre nombre de archivo, encabezado y referencias cruzadas. El orden es cronológico: **ADR-01** a **ADR-06** (persistencia, seguridad, DTOs, cartografía, auditoría y datos abiertos), **ADR-07** (geolocalización nativa), **ADR-08** (gestión del ciclo de vida), **ADR-09** (analítica agregada), **ADR-10** (notificaciones y duplicados), **ADR-11** (activos urbanos y resiliencia) y **ADR-12** (identificadores UUID y API v2).

## Índice

| ID | Origen | Design Doc | ADR relacionado | Implementación / evidencia |
|---|---|---|---|---|
| DD-001 | ADR | [Persistencia de enumeraciones con VARCHAR y CHECK](DD-001-persistencia-enums-jpa.md) | [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) | [Esquema SQL](../../database/init/diagramUrbanPulse.sql), [arquitectura](../architecture.md) |
| DD-002 | ADR | [Autenticación JWT, BCrypt y RBAC](DD-002-autenticacion-jwt-bcrypt-rbac.md) | [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) | [Feature 002](../../features/002-autenticacion-y-control-de-acceso-rbac/spec.md), [API](../api.md) |
| DD-003 | ADR | [DTOs y mappers dedicados](DD-003-desacoplamiento-dtos-mappers.md) | [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) | [Arquitectura, capa de mappers](../architecture.md) |
| DD-004 | ADR | [Cartografía con Leaflet y OpenStreetMap](DD-004-cartografia-interactiva-leaflet-openstreetmap.md) | [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md) | [Feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md) |
| DD-005 | ADR | [Trazabilidad inmutable de estados](DD-005-trazabilidad-inmutable-auditoria-estados.md) | [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md) | [Feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md), [esquema SQL](../../database/init/diagramUrbanPulse.sql) |
| DD-006 | ADR | [Datos Abiertos de Málaga y degradación controlada](DD-006-integracion-datos-abiertos-malaga-degradacion-controlada.md) | [ADR-06](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) | [Feature 004](../../features/004-reporte-y-seguimiento-de-incidencias/spec.md) |
| DD-007 | ADR | [Geolocalización nativa del navegador](DD-007-geolocalizacion-nativa-navegador.md) | [ADR-07](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) | [Feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md) |
| DD-008 | Feature | [Registro e inicio de sesión de ciudadanos](DD-008-registro-e-inicio-de-sesion-de-ciudadanos.md) | [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md), [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md), [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) | [Feature 001](../../features/001-registro-e-inicio-de-sesion-de-ciudadanos/spec.md) |
| DD-009 | Feature | [Autenticación y control de acceso RBAC](DD-009-autenticacion-y-control-de-acceso-rbac.md) | [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md), [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) | [Feature 002](../../features/002-autenticacion-y-control-de-acceso-rbac/spec.md) |
| DD-010 | Feature | [Mapa de incidencias y auditoría transversal](DD-010-mapa-incidencias-y-auditoria-transversal.md) | [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md), [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md), [ADR-07](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) | [Feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md), [QA note](../../docs/003-mapa-incidencias-y-auditoria-transversal/qa-note.md) |
| DD-011 | Feature | [Reporte y seguimiento de incidencias](DD-011-reporte-y-seguimiento-de-incidencias.md) | [ADR-06](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md), [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md), [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) | [Feature 004](../../features/004-reporte-y-seguimiento-de-incidencias/spec.md), [QA note](../004-reporte-y-seguimiento-de-incidencias/qa-note.md) |
| DD-012 | Feature | [Adjuntos multimedia en incidencias](DD-012-adjuntos-multimedia-en-incidencias.md) | [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md), [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) | [Feature 006](../../features/006-adjuntos-multimedia-en-incidencias/spec.md), [QA note](../006-adjuntos-multimedia-en-incidencias/qa-note.md) |

## Convenciones

- **Estado:** todos los DD se entregan en `En revisión`; no se marca `Aceptado` sin una revisión técnica real.
- **Umbrales:** solo se consignan los que aparecen en el repositorio (timeouts, paginación, validación de coordenadas, cobertura, healthchecks). Lo demás se registra como hipótesis o cuestión abierta.
- **Enlaces:** siempre relativos dentro del repositorio; se evitan rutas `file:///` y rutas absolutas de máquinas concretas.
- **Ciclo de vida:** el Design Doc es la propuesta/revisión del diseño; la documentación operativa vigente (`docs/api.md`, `docs/architecture.md`, `docs/deployment.md`) refleja el sistema implementado.
