---
name: sdd
description: Úsala siempre que trabajes con Spec-Driven Development en este proyecto (docs/constitution.md o cualquier archivo dentro de specs/) - redactar, revisar o cambiar specs, planes y tareas, o implementar y validar tareas de una spec.
---
# Spec-Driven Development (SDD)
## Flujo
Constitución → Spec → Clarificación(aplicable tras plan y tareas) → Plan → Tareas → Implementación → Validación →
Cambio.

## Normas
- Lee los archivos en `/constitution` y cada spec en `/features/NNN-nombre/spec.md` antes de empezar a trabajar en una feature, si no la has leido ya y las tienes en el contexto.
- Si necesitas aclarar algo, en `/docs/NNN-feature/` estará la documentación de la implementación de la feature, separadas por tareas. Si no está, pregunta al usuario antes de asumir nada.
- En `docs/NNN-feature/repare.md` se encuentran los problemas encontrados durante la validación de la feature. Debe eliminarse tras su uso en una implementación posterior. Si existe, debes tenerlo en cuenta antes de implementar de nuevo la feature.
- Nunca pases a la siguiente fase sin la aprobación explícita del usuario.
- Un cambio de requisitos se hace primero en la spec, luego en el plan y las tareas, y por último en el código.
- Cada spec vive en su carpeta: `/features/NNN-nombre` con `spec.md`, `plan.md` y `tasks.md`.
- La spec manda: si algo no está en la spec, no se implementa. 
- Si falta una decisión o necesitas más información, para y pregunta. 
- Al crear una feature, se añade a `/constitution/roadmap.md` (si no está) en la sección "Siguiente" y se crea la carpeta `/features/NNN-nombre-feature` con `spec.md`, `plan.md` y `tasks.md`.
- Si se pide implementar algo fuera de la spec, se actualiza `spec.md`, `plan.md` y `tasks.md` antes de implementarlo.
- Al comenzar una implementación, se moverá la feature a "En curso" en `constitution/roadmap.md`.
- Tras cada implementación, redacta en `/docs/NNN-feature/NN-task.md` un resumen de qué se ha hecho en esa tarea/tareas principales, incluyendo decisiones de diseño y problemas encontrados junto a la solución aplicada. Debe tener un apartado para cada subtarea de la tarea principal, además del global de la tarea principal. Debe ser un resumen que sirva como contexto a otro agente, así que debe tener suficiente información, pero sin sobrecargar el contexto.
- Tras acabar una tarea, marca la tarea como completada en `tasks.md`. Esto aplica a subtareas también, y debe hacerse en el orden de dependencia de las tareas.
- Tras cada implementación deben pasar los tests de código, y debe verse reflejada la cobertura de tests de cada feature en `/constitution/roadmap.md`.
- Al acabar la implementación de la feature, mover la feature a "En validación" en `constitution/roadmap.md`.
- Si falla la validación, se debe mover la feature a "En curso" en `/constitution/roadmap.md` y arreglar lo que haya fallado.
- Tras completar la implementación y validación, mover la feature a "Hecho" en `/constitution/roadmap.md`.
- El archivo `/docs/NNN-feature/qa-note.md` dará un resumen de la validación de la feature y de cómo debe probarse. Debe contener instrucciones para que cualquier persona pueda probar la feature manualmente, siendo lo más simples dentro de lo posible (sin pasos innecesarios ni jerga compleja), asegurando siempre cubrir todo lo necesario por probar (criterios de aceptación de la spec, casos límite y errores probados). Si algo no pudo probarse, debe indicarse junto al motivo.
- Durante la fase de validación, el veredicto y los hallazgos deben comunicarse obligatoriamente por el chat, informando con claridad si la validación ha finalizado con éxito o ha fracasado, qué ha fallado exactamente y qué aspectos hay que mejorar.
- Para realizar cambios en la spec, plan o tareas, se debe mover la feature a "En mejora" en `/constitution/roadmap.md`. Si está aquí, no se mueve a "En curso", se implementa y se valida, y tras ello se mueve a "Hecho" en `/constitution/roadmap.md`.
- Evita que se dupliquen specs en `roadmap.md`. Si hay una spec duplicada, pregunta al usuario antes de asumir nada. Si se ha movido una spec a, por ejemplo, "En mejora", debe borrarse la otra spec duplicada de "Siguiente" o "En curso" en `roadmap.md` para que no esté duplicado y lleve a dudas.
- Las plantillas funcionan así: Sustituye todo lo que esté entre `<…>` y borra las notas en _cursiva_.
