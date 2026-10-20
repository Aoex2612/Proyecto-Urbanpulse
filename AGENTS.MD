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
- Mensajes de commit y documentación: Español técnico o inglés coherente con el equipo.
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
