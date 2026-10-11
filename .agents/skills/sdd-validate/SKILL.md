---
name: sdd-validate
description: Usa esta skill para validar una spec en sdd. Se indicará el número de la feature, el mombre o ambos.
---

# Validación de una Spec

## Objetivo
Validación de una spec revisando los test, que cumpla los requisitos y los criterios de aceptación

## Entradas
- Número de la feature, nombre o ambas cosas.

## Salidas
- En caso de éxito en la validación, se genera `qa-note.md`.
- En caso de fallo en la validación, se genera el archivo auxiliar `repare.md`.

## Proceso
1. Recorre `/feature/NNN-nombre/spec.md` requisito por requisito. Para cada RF indica qué tests lo cubren y el resultado de ejecutarlo. 
2. Si algún RF no está cubierto o falla, dilo claramente y da la validación por fallada y continua.
3. Revisa los requisitos no funcionales, y en caso de que uno no se cumpla, da por fallida la validación.
4. Revisa todo el código relacionado con la implementación. Si falla algo, da la validación por fallada.
5. Después comprueba los criterios de finalización y dame un veredicto: ¿la spec está cumplida?
6. Si no se cumplen, desmarca las tareas que fallán y da la validación por fallada.

## Reglas
- Si algo falla en alguno de los pasos del proceso, es decir, se dar por fallada la validación, NO arregles nada todavía. Apuntalo en el archivo auxiliar `repare.md` y continua el siguiente paso.
- Usa la skill sdd.
- - Actua como un QA profesional.