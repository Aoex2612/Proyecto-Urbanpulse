# AGENTS.md — <proyecto>

Plataforma cloud para la gestión inteligente de incidencias urbanas. Recibe información de reportes enviados por los ciudadanos con datos contextuales de la ciudad para mejorar su clasificación, priorización, seguimiento, análisis y resolución. (clima, tráfico, activos urbanos)

## Estructura del proyecto
- `frontend/` — Aplicación web (React, Vite, ESLint, assets y configuración del cliente)
- `Backend/` — Servicio backend en Java con Spring Boot y Maven (`pom.xml`, código fuente en `src/` y compilación en `target/`)
- `features/` — Especificaciones, artefactos o documentación de las diferentes funcionalidades (`NNN-nombre-feature`)
- `docs/` — Documentación técnica del proyecto, incluyendo registros de decisiones de arquitectura (`docs/ADR/`)
- `constitution/` — Reglas clave, hoja de ruta, stack tecnológico y misión del proyecto (`tech-stack.md`, `roadmap.md`, `mission.md`)
- `.github/` — Workflows de integración continua (CI/CD), plantillas de PR e issues para GitHub

## Comandos 
### Backend (Java/Spring Boot)
- Ejecutar: `cd Backend && mvn spring-boot:run`
- Tests: `cd Backend && mvn test`
- Lint/formato: `cd Backend && mvn verify`

### Frontend (react/vite)
- **Backend:** Java 21 con Spring Boot. Código y comentarios en inglés.
- **Frontend:** React + Vite con configuración de ESLint.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`).
- **Ramas:** GitHub Flow (`feature/nombre-feature`, `fix/nombre-bug`).


## Estilo y convenciones
- **Backend:** Java 21 con Spring Boot. Comentar cuando sea necesario.
- **Frontend:** React + Vite con configuración de ESLint.
- **Commits:** Commits convencionales (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`).
- **Ramas:** GitHub Flow (`feature/nombre-feature`, `fix/nombre-bug`).


## Flujo de trabajo
- Antes de una tarea no trivial, propón un plan y espera mi OK.
- Una tarea a la vez; al terminar, dime qué cambiaste para que lo revise.
- Si no estás seguro al 80%, pregunta. No inventes.

## Al terminar cualquier tarea
- Ejecutar validaciones correspondientes (`mvn verify` en Backend y `npm run lint` / tests en Frontend).
- Comprobar que no se suban archivos de configuración personal (`.idea/`, `.kate-swp`, etc.) ni credenciales.

## Documentación
- Reglas y decisiones técnicas detalladas en `constitution/` (`tech-stack.md`, `roadmap.md`, `mission.md`).
- Histórico de decisiones de arquitectura en `docs/ADR/`.
- Especificaciones de casos de uso/funcionalidades en `features/`.
