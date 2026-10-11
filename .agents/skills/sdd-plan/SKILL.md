---
name: sdd-plan
description: Esta skill se usará a la hora de crear el plan a seguir para una feature con el flujo sdd.
---

# Creación de un plan para una feature

## Objetivo
Creación del plan a seguir para implementar una feature en el ciclo sdd.

## Entradas
- Número de la feature, nombre o ambas cosas.
- Información extra.
- `spec.md` y `/constitution`.

## Salidas
- Archivo `plan.md`.

## Proceso
1. Lee la información de la feature.
2. Identifica: Archivos que se crean/modifican y su responsabilidad, funciones a crear y requisitos relacionados, cómo se pinta en la interfaz, decisiones técnicas justificadas (con su alternativa descartada) y estrategia de tests.
3. Rellena la plantilla en `./assets` según la información que tienes.

## Reglas
- Si la spec no está aprobada o tiene dudas abiertas, para y avísame.
- No escribas código
- Usa la skill sdd.
- Debe cubrir todos los requisitos.