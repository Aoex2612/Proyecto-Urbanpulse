---
name: sdd-task
description: Skill que se usará cuando se quiera crear las tareas para implementar una feature a partir de su spec y el plan usando sdd.
---

# Division del plan en tareas

## Objetivo
Creación de `task.md` a partir de la spec y el plan.

## Entradas
- Número de la feature, nombre o ambas cosas.
- Información extra.
- Archivos `spec.md` y `plan.md`

## Salidas
- Archivo `task.md`.

## Proceso
1. Lee la información de la feature y divide en tareas y subtareas pequeñas y verificables.
2. Rellena siguiendo la plantilla dentro de `./assets`.
3. En cada tarea principal, indica los requisitos que corresponden a dicha tarea.
4. Si falta algo en `plan.md` o `spec.md` para y pregunta.


## Reglas
- Máximo 30 min por tarea principal, en orden de dependencia. 
- Si salen más de 10 tareas principales, puedes proponer dividir la spec si tiene sentido.
- Si una tarea es dependiente de otra, indicalo.
- Usa la skill sdd.
- No inventes