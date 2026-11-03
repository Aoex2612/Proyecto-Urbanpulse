---
name: sdd-status
description: Usa esta skill cuando se pregunte por fase actual y siguiente paso de una spec al usar sdd.
---
# Estado de una Spec

## Objetivo
Validación de una spec revisando los test, que cumpla los requisitos y los criterios de aceptación

## Entradas
- Número de la feature, nombre o ambas cosas.

## Proceso
Lee la información de la feature y `roadmap.md` y di en pocas líneas:
1. En qué fase del flujo SDD está esta spec y su estado. Indica tambien si está en implementación tras validación fallada (si existe `repare.md`)
2. Tareas hechas y pendientes (x de y).
3. El siguiente paso exacto, con el comando /sdd-* que debo ejecutar.

## Reglas
- No modifiques ningún archivo.
- Usa la skill sdd.