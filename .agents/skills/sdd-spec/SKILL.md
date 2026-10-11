---
name: sdd-spec
description: Usa esta skill cuando el usuario pida crear y redactar una especificación (spec) de una feature. Guía una entrevista de requisitos y produce un spec.md siguiendo la plantilla del equipo.
---

# Generador de specs

## Objetivo
Convierte una idea vaga en una especificación acordada. La spec es elcontrato: si algo no está aquí, no se implementa.

## Entradas
- Información de la nueva feature.

## Salidas
- Archivo `spec.md`.

## Proceso
1. **Entrevista al usuario.** Preguntas de **UNA en UNA**, máximo 15, esperando respuesta antes de la siguiente. Céntrate en casos límite, comportamiento ante errores y qué queda fuera. No propongas soluciones técnicas: si el    usuario pregunta "¿cómo lo harías?", redirige al QUÉ. Prioriza preguntas cuya respuesta cambie lo que hay que construir; descarta las que tengan una respuesta obvia por defecto.
2. **Elige el número.** Mira `features/` y usa el siguiente libre con tres dígitos: `features/NNN-<nombre-en-kebab-case>/spec.md`.
3. **Redacta** usando la plantilla en `./assets`, sin saltarte secciones. Criterios de aceptación **siempre en notación EARS**, numerados como RF-1, RF-2, … Cada requisito debe ser verificable: si no se te ocurre cómo comprobarlo, está mal escrito.
4. **Marca lo que no sepas** como `[NECESITA ACLARACIÓN: pregunta concreta]`. Nunca rellenes un hueco inventando: un hueco visible es información, una suposición silenciosa es deuda.
5. **Pide aprobación explícita** al terminar. No pases al plan ni escribas código hasta tenerla.
6. Indica si se necesitas mas preguntas para aclarar la spec, o si ya está lista para pasar al plan.

## Reglas
- No escribas código.
- Usa la skill sdd.
- La spec describe **QUÉ** y **POR QUÉ**. Prohibido incluir stack, arquitectura, nombres de archivos, esquemas de datos, algoritmos o firmas de funciones: eso va en el plan.
- Incluye **siempre** la sección "Fuera de alcance". Es la que evita que la funcionalidad crezca sola.
- Un requisito, una frase. Si necesitas un "y" para unir dos comportamientos, son dos requisitos.
- Sin adjetivos no medibles: "rápido", "intuitivo", "robusto" no son requisitos. Escribe el umbral o no lo escribas.

## Notación EARS

Cinco patrones. Elige el que corresponda, no mezcles:

| Patrón | Forma | Cuándo |
|---|---|---|
| Ubicuo | EL SISTEMA \<hará\> | siempre cierto |
| Dirigido por evento | CUANDO \<disparador\>, EL SISTEMA \<hará\> | responde a algo |
| Estado | MIENTRAS \<estado\>, EL SISTEMA \<hará\> | durante una condición |
| Opcional | DONDE \<característica\>, EL SISTEMA \<hará\> | solo si está presente |
| No deseado | SI \<condición\>, ENTONCES EL SISTEMA \<hará\> | errores y casos límite |

Ejemplo bien escrito:

> RF-4: SI el nombre ya existe (comparación ignorando mayúsculas y espacios
> exteriores), ENTONCES EL SISTEMA no creará un duplicado e informará del
> conflicto (salida 1).

Mal escrito, para contrastar:

> ~~RF-4: El sistema debe manejar bien los duplicados y ser rápido.~~
> Sin patrón EARS, sin criterio verificable, dos ideas en una frase y un
> adjetivo no medible.