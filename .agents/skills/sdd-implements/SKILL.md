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
- Resumen global de la implementación (paso 5 del proceso).

## Proceso
1. Revisa las tareas en `tasks.md` para saber si hay alguna bloqueada por otra y verificar el orden de dependencias.
2. Identifica las tareas ejecutables de implementación. Si detectas tareas de validación final o verificación de criterios de aceptación de la spec, resérvalas para la fase posterior de validación (`sdd-validate`) y NO las lances en un subagente.
3. Invoca los subagentes para las tareas de implementación que se pueden realizar con las siguientes indicaciones:
    0. Ten en cuenta la skill sdd.
    1. PROHIBICIÓN: No invoques ni llames a otros subagentes bajo ninguna circunstancia. Realiza todo el trabajo asignado tú directamente.
    2. Escribe los tests necesarios para comprobar lo que se va a implementar.
    3. Escribe el código hasta que pasen los tests y envía el resultado de su ejecución al agente principal.
    4. Realiza commits siguiendo la norma de commits del proyecto indicando lo que se ha hecho cada vez que sea necesario.
    5. Marca la tarea (o subtareas si no se completó toda la tarea) como hecha en `tasks.md` e indica qué RF cubre.
    6. Redacta la documentación necesaria en `/docs/NNN-feature/NN-tarea.md`.
    7. Avisa de tareas incompletas o errores al agente principal.
4. Con las tareas desbloqueadas, vuelve a invocar a los subagentes necesarios con las indicaciones anteriores.
5. Finalmente, resume las tareas completas e incompletas, los errores encontrados y si es necesaria la intervención del usuario.

## Reglas
- Para cada tarea principal de desarrollo/implementación debes llamar a un subagente que la realice.
- **Prohibido el anidamiento de subagentes:** Los subagentes NO deben llamar ni invocar a otros subagentes. Solo el agente principal orquesta y lanza subagentes.
- **Exclusión de la verificación de criterios de aceptación en subagentes:** La verificación y validación de los criterios de aceptación de la spec NO se harán en ningún subagente ni durante la fase de implementación. Aunque figuren como tarea o subtarea en `tasks.md`, dicha comprobación se reserva exclusivamente para la fase posterior de validación (`sdd-validate`), orquestada por el agente principal.
- Si una tarea es dependiente de otra, espera a que se complete antes de lanzar el subagente de la tarea bloqueada.
- Si no se indica otra cosa en `AGENTS.md`, se supondrá que la norma de los commits será Conventional Commits en español.
- Usa la skill sdd.

## Conventional Commits en español

Varios tipos. Elige el que corresponda, no mezcles:

| Prefijo | Cuándo |
|---|---|
| feat | Una nueva característica o funcionalidad para el usuario |
| fix | Corrección de un error o bug |
| docs | Cambios exclusivos en la documentación |
| style | Cambios de formato, espaciado o estilo que no afectan la lógica del código |
| refactor | Modificación de código que no corrige un error ni añade nueva funcionalidad |
| perf | Mejora de rendimiento del código |
| test | Añadir o corregir pruebas |
| build | Cambios en el sistema de construcción o dependencias externas |
| ci | Cambios en scripts o flujos de integración continua |
| chore | Mantenimiento o tareas que no modifican código fuente ni tests |
| ops | Operaciones de infraestructura, despliegue, backups o monitorización |

Plantilla:
> `<tipo>(<alcance>)[!]: <descripción concisa y clara en español>`
>
> `[cuerpo opcional con detalles adicionales del cambio]`
>
> `[BREAKING CHANGE: explicación del cambio incompatible]`

Elementos opcionales:
- **Scope `(<alcance>)`:** Se coloca entre paréntesis inmediatamente después del tipo para delimitar el módulo, componente o capa afectada (por ejemplo: `fix(incidencias):`, `test(db):`).
- **`!` de Breaking Change:** Se añade justo antes de los dos puntos (ejemplo: `feat!:`) para avisar visual y rápidamente de un cambio que rompe la compatibilidad hacia atrás.
- **Pie `BREAKING CHANGE:`:** Si se rompe compatibilidad, se añade al final del mensaje explicando exactamente qué funcionalidad cambia y cómo migrar o adaptarse.

Ejemplos bien escritos:
> `feat: Completada la tarea 3 de la feature 5, creación de la función de login con usuario y contraseña, y tests unitarios para la misma.`
>
> `feat(auth): integración de filtro JWT y validación de tokens en endpoints protegidos`
>
> `refactor(api)!: modificación de endpoints REST de incidencias para requerir paginación obligatoria`
> `BREAKING CHANGE: Las peticiones a /api/incidents ahora requieren page y size en la query.`
