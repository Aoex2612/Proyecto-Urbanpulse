---
name: sdd-clarify
description: Utiliza esta skill cuando debas revisar los documentos de una features usando sdd, es decir spec, task y plan.
---

# Clarificación de una Spec

## Objetivo
Validación de la correcta creación de `spec.md`, `plan.md` o `task.md`.

## Entradas
- Número de la feature, nombre o ambas cosas.
- Archivo de la feature a revisar, si no se indica ninguno, suponer que es `spec.md`

## Salidas
- Lista numerada.
- Pregunta al usuario sobre si le parece bien y si quiere que se cambie lo detectado.

## Proceso
0. Revisa el documento indicado y crea una lista con los siguientes elementos:
Para spec:
1. Ambigüedades restantes (requisitos que no se pueden verificar).
2. Contradicciones entre requisitos.
3. Casos límite no cubiertos.
4. Conflictos con `/constitution`.
Para task:
1. Tareas faltantes.
2. Tareas contradictorias
3. Tareas bloqueantes no indicadas.
4. Conflictos con `/constitution`, `spec.md` o `plan.md`.
Para plan:
1. Enfoque erroneo.
2. Decisiones contradictorias
3. Riesgos no tenidos en cuenta.
4. Estrategia de test errornea
5. Conflictos con `/constitution` o `spec.md`.

## Reglas
- Usa la skill sdd.
- Actua como un QA profesional.
- Solo detecta, no resuelvas ni propongas nada.