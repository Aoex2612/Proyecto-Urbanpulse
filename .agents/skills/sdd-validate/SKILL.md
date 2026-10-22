---
name: sdd-validate
description: Usa esta skill para validar una spec en sdd. Se indicará el número de la feature, el nombre o ambos.
---

# Validación de una Spec

## Objetivo
Validación exhaustiva de una feature/spec revisando los tests, que cumpla los requisitos funcionales y no funcionales (RF y RNF), y los criterios de aceptación.

## Entradas
- Número de la feature, nombre o ambas cosas.
- Información extra.

## Salidas
- **Comunicación en el chat (obligatoria):** Veredicto claro (si la validación ha finalizado con éxito o ha fracasado), qué ha fallado exactamente o qué aspectos hay que mejorar, junto con un resumen de lo validado.
- En caso de éxito en la validación, se genera `/docs/NNN-feature/qa-note.md` siguiendo las normas de `sdd`.
- En caso de fallo en la validación, se genera el archivo auxiliar `/docs/NNN-feature/repare.md`.

## Proceso
1. Recorre `/features/NNN-nombre/spec.md` requisito por requisito. Para cada RF indica qué tests lo cubren y el resultado de ejecutarlo.
2. Si algún RF no está cubierto o falla, dilo claramente, apúntalo en `repare.md`, da la validación por fallada y continúa.
3. Revisa los requisitos no funcionales, y en caso de que uno no se cumpla, apúntalo en `repare.md` y da por fallida la validación.
4. Revisa todo el código relacionado con la implementación y ejecuta los tests. Si falla algo, apúntalo en `repare.md` y da la validación por fallada.
5. Después comprueba los criterios de finalización y emite un veredicto: ¿la spec está cumplida?
6. Si no se cumplen, desmarca las tareas que fallan en `tasks.md`, documéntalas en `repare.md` y da la validación por fallada.
7. **Comunica el veredicto en el chat y DETENTE:**
   - Detalla si la validación ha finalizado con éxito o ha fracasado, qué ha fallado y qué recomendaciones o mejoras se detectaron. 
   - Informa explícitamente al usuario y esspera la confirmación.
   - **IMPORTANTE:** NO ejecutes el paso 8 ni toques `/docs/NNN-feature/qa-note.md` bajo ninguna circunstancia hasta recibir un mensaje explícito de confirmación en el chat. Da por finalizado tu turno aquí.
8. **Tras recibir confirmación expresa del usuario:**
   - Redacta `/docs/NNN-feature/qa-note.md`, si ha acabado con éxito los tests y al menos el 70% de las tareas, RF y RNF, y criterios de finalización están cumplidos.
   - Sigue la norma de la skill `sdd`: debe incluir el resumen de la validación y las instrucciones para que cualquier persona pueda probarla manualmente de la forma más simple posible (sin rodeos ni pasos superfluos, pero cubriendo todo lo necesario).

## Reglas
- **Comunicación en el chat:** Nunca limites tu respuesta a decir que se ha generado el archivo. Debes informar siempre en el chat del veredicto, de los fallos encontrados y de las mejoras sugeridas.
- **Instrucciones manuales simples:** La guía de pruebas manuales en `qa-note.md` debe ser lo más simple posible para una persona, asegurando siempre que se cubra todo lo necesario para verificar la funcionalidad.
- Si algo falla en alguno de los pasos del proceso, es decir, se da por fallada la validación, NO arregles nada todavía. Apúntalo en el archivo auxiliar `repare.md`, continúa el siguiente paso y repórtalo en el chat.
- Usa la skill sdd.
- Actúa como un QA profesional.