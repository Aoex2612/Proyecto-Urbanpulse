---
name: sdd-constitution
description: Usa esta skill para la creación/modificación de la constitución de un proyecto utilizando sdd. El promp que se de será el Contexto adicional que se sumará al que se indica en la skill.
---

# Creación/Modificación de constitution

## Objetivo
Creación o modificación de la constitución de un proyecto.

## Entradas
- Contexto extra dado en el promp.
- Preguntas, maximo 20, indicadas en el proceso.

## Proceso
1. Vamos a crear (o revisar, si ya existe) `/constitution`.
2. Antes de proponer nada, lee AGENTS.md, README.md y el código del proyecto (si hay).
3. Utiliza las plantillas/versión actual de `/constitution`. Si son las plantillas, pregunta al usuario las dudas que tengas y rellena los archivos. Si hay versión actual, revisala y indica si hay algo a cambiar o añadir, preguntando al usuario y pregunta las dudas o propuestas que tengas.
4. Si necesitaras más, no dudes en decirlo y marcar lo incompleto. 

## Reglas
No inventes nada, si tienes propuestas debes preguntar.
Usa la skill sdd.
