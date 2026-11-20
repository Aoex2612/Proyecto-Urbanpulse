# Detección y Sugerencia de Incidencias Duplicadas (HU20)

## Resumen del Componente
Este componente implementa el motor determinista de detección espaciotemporal y semántica de incidencias urbanas potencialmente duplicadas (`DuplicateDetectionService`), alertando a perfiles operativos y técnicos ante la presencia de avisos solapados (RF-10, RF-11).

## Criterios Deterministas de Detección
1. **Filtro Temporal:** Solo se cotejan incidencias registradas en las últimas 48 horas (`MAX_HOURS_DIFF = 48L`).
2. **Filtro Temático:** Se exige igualdad estricta de categoría temático-funcional (`category.equalsIgnoreCase`).
3. **Filtro Espacial:** Se calcula la distancia geodésica mediante la fórmula de Haversine con radio terrestre de 6.371 km, acotando el umbral a un radio máximo de 60 metros (`MAX_DISTANCE_METERS = 60.0`).
4. **Filtro Léxico-Semántico:** Se normaliza el texto combinado (título y descripción eliminando signos ortográficos, tildes y pasando a minúsculas) calculando el coeficiente de Jaccard sobre conjuntos de palabras (`SIMILARITY_THRESHOLD = 0.3`).

## Emisión de Alertas
- **Destinatarios Exclusivos:** Se consultan los usuarios con roles `OPERATOR` y `TECHNICIAN` en `UserRepository`. Los roles `CITIZEN`, `ADMIN` y `ANALYST` no reciben estas alertas de triaje.
- **Tipo de Notificación:** `DUPLICATE_SUGGESTION`.
- **Mensaje:** `La incidencia '{titulo}' (id: {id}) puede ser duplicada de {N} incidencia(s) existente(s).`

## Resolución y Confirmación
- Endpoint expuesto: `POST /api/incidents/{id}/mark-duplicate`.
- Transiciona la incidencia duplicada al estado `REJECTED` consignando el motivo de duplicidad en la trazabilidad de auditoría.
