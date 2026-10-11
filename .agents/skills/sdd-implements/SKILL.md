---
name: sdd-implements
description: Skill que se usará cuando se quiera implementar una feature usando sdd a partir de sus tareas.
---

# Implementación de una Spec

## Objetivo
Implementación de las tareas de una feature en el ciclo sdd.

## Entradas
- Número de la feature, nombre o ambas cosas.
- Información extra.

## Salidas
- Resumen de cada implementación en `NN-task.md`.
- Resumen global de la implementación(paso 4 del proceso)

## Proceso
1. Revisa las tareas para sabes si hay alguna bloqueada por otra.
2. Invoca los subagentes para las tareas que se pueden hacer con las siguientes indicaciones:
    0. Ten en cuenta la skill sdd.
    1. Escribe los test necesarios para comprobar lo que se va a implementar.
    2. Escribe el código hasta que pasen los test. Y envía el resultado de su ejecución al agente principal.
    3. Marca la tarea(o subtareas si no se completó toda la tarea) como hecha en tasks.md e indica qué RF cubre.
    4. Redacta la documentación necesaria en `/dosc/NNN-feature/NN-tarea.md`.
    5. Avisa de tareas incompletas o errores al agente principal.
3. Ahora, con las tareas desbloqueadas, vuelve a invocar a los subagentes necesarios con las indicaciones anteriores.
4. Finalmente, resume las tareas completas e incompletas y los errores encontrados y si el es necesaria la intervención del usuario.


## Reglas
- Para cada tarea principal debes llamar a un subagente que la realice.
- Si una tarea es dependiente de otra, espera a que se complete antes de lanzar el subagente de la tarea bloqueada
- Usa la skill sdd.