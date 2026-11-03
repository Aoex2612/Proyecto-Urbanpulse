---
name: sdd-change
description: Utiliza esta skill cuando se quiera cambiar algo de una feature ya creada, es decir, modificar la especificación del flujo sdd.
---

# Cambio de una Spec

## Objetivo
Cambio en una feature existente con un nuevo requisito.

## Entradas
- Número de la feature, nombre o ambas cosas.
- Nuevos requisitos a implementar.

## Salidas
- Cambio en `spec.md`, `plan.md`, `task.md`.

## Proceso
1. Actualiza `spec.md` de la feature indicada, si pertenece a otra pregunta antes de hacer nada: añade nuevo RF (o cambio de uno existente) en EARS, sus casos límite y lo que queda fuera de alcance.
2. Indica qué partes de plan.md y tasks.md habría que cambiar después.
3. Muéstrame el diff de la spec y espera mi aprobación. Esto se aplica entre los siguientes pasos.
4. Aplica los cambios (si se han aceptado) y realiza la clarificación de `spec.md` .
5. Aplica los cambios (si se han aceptado) y realiza la clarificación de `plan.md` .
6. Aplica los cambios (si se han aceptado) y realiza la clarificación de `task.md` . Además si hay acciones recurrentes, desmarcalas.

## Reglas
- NO toques código.
- Usa la skill sdd.
- Ayudate de la skill sdd-clarify, sdd-spec, sdd-plan y sdd-task.
