# AGENTS.md — UrbanPulse

Plataforma integral para la gestión, monitorización y resolución de incidencias urbanas en tiempo real. Arquitectura desacoplada con backend en Java/Spring Boot, frontend en React/Vite y persistencia en PostgreSQL.

## Estructura del proyecto

- `Backend/` — Código fuente del servidor (Spring Boot, Java, Maven).
- `frontend/` — Aplicación web cliente (React 19, Vite).
- `database/` — Configuración Docker y scripts de inicialización SQL (`diagramUrbanPulse.sql`).
- `constitution/` — Directrices de gobernanza, visión técnica, principios y roadmap del proyecto.
- `docs/` — Documentación técnica de diseño y Architectural Decision Records (ADRs).
- `features/` — Especificaciones (`spec.md`), planes técnicos (`plan.md`) y desglose de tareas (`tasks.md`) de cada funcionalidad siguiendo SDD.

## Comandos

- BD Local: `docker compose up -d`
- Backend Tests: `cd Backend && mvn clean test`
- Backend Dev: `cd Backend && mvn spring-boot:run`
- Frontend Dev: `cd frontend && npm run dev`
- Frontend Lint: `cd frontend && npm run lint`

## Estilo y convenciones

- Lenguaje: Java 17/21 para backend; JavaScript/React para frontend.
- Código y nombres: Clases en PascalCase, métodos/variables en camelCase, constantes en UPPER_CASE.
- **Mensajes de commit:** Obligatorio seguir el estándar de **Conventional Commits** con el **mensaje en español** (ejemplos: `feat: carpeta creada`, `fix: corrección de validación de token`, `test: pruebas unitarias de filtro de seguridad`, `docs: actualización de especificación`).
- **Documentación de código:** Comentar obligatoriamente con **Javadoc** en el backend (clases, métodos, parámetros `@param`, retornos `@return` y excepciones `@throws`) y de forma análoga con **JSDoc** en el frontend (componentes, hooks, funciones utilitarias y servicios, indicando parámetros `@param` y retornos `@returns`).

## Flujo de trabajo (SDD)

- Antes de una tarea no trivial, propón un plan y espera mi OK.
- Una tarea a la vez; al terminar, dime qué cambiaste para que lo revise.
- Si no estás seguro al 80%, pregunta. No inventes.
- Ninguna feature se implementa sin su correspondiente especificación (`spec.md`) aprobada.

## Al terminar cualquier tarea

- Verificar que los tests unitarios y de integración pasen (`mvn test`, `npm run lint`).
- Documentar los cambios realizados e indicar el siguiente paso.

## Documentación

- [Misión y Gobernanza](constitution/mission.md)
- [Tech Stack y Convenciones](constitution/tech-stack.md)
- [Roadmap](constitution/roadmap.md)
- [README Principal](README.md)

## Gestión de Git y Commits

- **Herramientas:** Realizar las operaciones de Git (crear ramas, commits y pull requests) preferentemente mediante el **MCP de GitHub**. Si no se encuentra disponible o falla, ejecutar directamente los comandos de terminal (`git`).
- **Flujo de ramas:** Toda nueva rama debe partir obligatoriamente desde `develop` y su correspondiente Pull Request debe apuntar hacia `develop`.
- **Idioma y caracteres:** 
  - Redactar mensajes de commit, nombres de ramas y PRs en **español**.
  - Si una palabra lleva la letra `ñ`, sustituirla obligatoriamente por `n` (por ejemplo: `diseno`, `anadir`).
  - No usar tildes ni caracteres especiales en ramas o comandos.
- **Nomenclatura de ramas:** Formato `<tipo>/<descripcion-kebab-case>`:
  - `feature/...`: Nueva funcionalidad
  - `fix/...`: Corrección de bug
  - `docs/...`: Documentación
  - `refactor/...`: Refactorización
  - `test/...`: Pruebas
  - `hotfix/...`: Corrección urgente en producción
- **Convención de commits:** Seguir **Conventional Commits** en español:
  - `feat`: Nueva funcionalidad
  - `fix`: Corrección de errores
  - `docs`: Documentación
  - `test`: Pruebas unitarias o de integración
  - `refactor`: Cambios en código que no alteran el comportamiento
  - `chore`: Tareas de mantenimiento, configuración o dependencias
  - *Ejemplo:* `feat: anadir endpoint para creacion de incidencias`


