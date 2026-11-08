# URBANPULSE - CONTRATO CANÓNICO DE API (SPRINT 1)
Documento formal de interfaz REST para el intercambio de datos entre Frontend y Backend.

---

## 1. ENDPOINTS CORE DEL SPRINT 1

Para el Sprint 1 (MVP de 2 semanas) se definen exclusivamente los cuatro endpoints indispensables para cerrar el ciclo de vida inicial:
1. `POST /api/incidents`: Alta del aviso por el ciudadano.
2. `GET /api/incidents/my-incidents`: Seguimiento de avisos del ciudadano autenticado.
3. `GET /api/incidents/{id}`: Consulta de detalle de una incidencia.
4. `PATCH /api/incidents/{id}/status`: Triaje del operador (Validación o Rechazo).

---

## 2. ESPECIFICACIÓN DETALLADA POR ENDPOINT

### 2.1. POST /api/incidents (Crear Incidencia - RF03, RF04)
Permite a un usuario con rol `CITIZEN` registrar una nueva incidencia.

#### Cabeceras
- `Authorization`: `Bearer <token_jwt>` (Obligatorio)
- `Content-Type`: `application/json`

#### Payload Enviado por el Formulario (Request Body)
```json
{
  "title": "Farola rota que chispea en el parque",
  "description": "La farola junto al banco de madera tiene cables sueltos y echa chispas.",
  "category": "ALUMBRADO",
  "latitude": 36.7196,
  "longitude": -4.4200,
  "accuracyMeters": 4.5,
  "address": "Calle Marqués de Larios, 5"
}
```

#### Parámetros del Formulario, Restricciones y Justificación
| Campo | Tipo | Obligatorio | Mín / Máx | Justificación del Límite ("Por qué") |
| :--- | :--- | :---: | :--- | :--- |
| `title` | String | **Sí** | Mín: 5 caracteres<br>Máx: 200 caracteres | La BD define `varchar(200)`. Mínimo 5 caracteres para evitar títulos vacíos o irrelevantes (ej. "hola"). |
| `description` | String | **Sí** | Mín: 10 caracteres<br>Máx: Sin límite (`TEXT`) | En BD es `TEXT`. Requiere al menos 10 caracteres para garantizar una explicación mínima comprensible de la avería. |
| `category` | String | **Sí** | Máx: 50 caracteres | Corresponde al tamaño `varchar(50)` de la BD y al selector de categorías funcionales (`ALUMBRADO`, `TRAFICO`, etc.). |
| `latitude` | Number | **Sí** | Mín: 36.5000<br>Máx: 36.9000 | Coordenadas decimales WGS84 (`double precision`). El rango se acota al término municipal y metropolitano de Málaga para descartar coordenadas fuera de ámbito. |
| `longitude` | Number | **Sí** | Mín: -4.7000<br>Máx: -4.2000 | Coordenadas decimales WGS84 (`double precision`) correspondientes al área geográfica de Málaga. |
| `accuracyMeters` | Number | No | Mín: 0.0<br>Máx: 500.0 | Proviene de la API Geolocation del navegador. Mayor a 500 metros se considera imprecisión excesiva para gestión en calle. |
| `address` | String | No | Máx: 255 caracteres | En BD la columna es `varchar(255)`. Opcional si se resuelve por geocodificación inversa o texto introducido por el usuario. |

*Nota arquitectónica:* El formulario **nunca** envía `citizen_id`, `status` ni `priority`. El backend extrae la identidad del usuario del JWT y asigna automáticamente `status = 'REPORTED'` y `priority = 'MEDIUM'`.

#### Respuesta del Backend (201 Created)
```json
{
  "id": 101,
  "title": "Farola rota que chispea en el parque",
  "description": "La farola junto al banco de madera tiene cables sueltos y echa chispas.",
  "category": "ALUMBRADO",
  "latitude": 36.7196,
  "longitude": -4.4200,
  "accuracyMeters": 4.5,
  "address": "Calle Marqués de Larios, 5",
  "district": "Centro",
  "neighborhood": "Centro Histórico",
  "status": "REPORTED",
  "priority": "MEDIUM",
  "priorityReason": null,
  "citizenId": 42,
  "createdAt": "2026-10-20T18:30:00Z",
  "updatedAt": "2026-10-20T18:30:00Z",
  "attachments": []
}
```

#### Respuestas de Error
- **`400 Bad Request`**: Cuando fallan validaciones de longitud, falta un campo obligatorio o las coordenadas no son números válidos.
- **`401 Unauthorized`**: Falta de token JWT o firma expirada.

---

### 2.2. GET /api/incidents/my-incidents (Listado del Ciudadano - RF06)
Devuelve únicamente los avisos registrados por el ciudadano en sesión.

#### Cabeceras
- `Authorization`: `Bearer <token_jwt>` (Obligatorio)

#### Parámetros de Consulta (Query Params)
| Parámetro | Tipo | Obligatorio | Valores / Rango | Por qué |
| :--- | :--- | :---: | :--- | :--- |
| `page` | Integer | No | Mín: 0 (Default: 0) | Paginación en servidor para no colapsar la respuesta. |
| `size` | Integer | No | Mín: 1, Máx: 50 (Default: 10) | Evitar sobrecarga de memoria en frontend y backend. |

#### Respuesta del Backend (200 OK)
```json
{
  "content": [
    {
      "id": 101,
      "title": "Farola rota que chispea en el parque",
      "category": "ALUMBRADO",
      "status": "REPORTED",
      "priority": "MEDIUM",
      "address": "Calle Marqués de Larios, 5",
      "district": "Centro",
      "createdAt": "2026-10-20T18:30:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 10
}
```

---

### 2.3. GET /api/incidents/{id} (Detalle de Incidencia - RF06)
Consulta la información completa de una incidencia y sus evidencias asociadas.

#### Parámetros de Ruta
- `id`: Long (Mín: 1) -> `BIGSERIAL` en BD.

#### Respuesta del Backend (200 OK)
```json
{
  "id": 101,
  "title": "Farola rota que chispea en el parque",
  "description": "La farola junto al banco de madera tiene cables sueltos y echa chispas.",
  "category": "ALUMBRADO",
  "latitude": 36.7196,
  "longitude": -4.4200,
  "accuracyMeters": 4.5,
  "address": "Calle Marqués de Larios, 5",
  "district": "Centro",
  "neighborhood": "Centro Histórico",
  "status": "REPORTED",
  "priority": "MEDIUM",
  "priorityReason": null,
  "citizenId": 42,
  "createdAt": "2026-10-20T18:30:00Z",
  "updatedAt": "2026-10-20T18:30:00Z",
  "attachments": [
    {
      "id": 1,
      "fileName": "farola_chispas.jpg",
      "fileUrl": "/uploads/101_farola_chispas.jpg",
      "mimeType": "image/jpeg",
      "fileSizeBytes": 2048500,
      "uploadedAt": "2026-10-20T18:31:00Z"
    }
  ]
}
```

#### Respuestas de Error
- **`404 Not Found`**: La incidencia con dicho `id` no existe en la BD.
- **`403 Forbidden`**: Si un ciudadano intenta consultar una incidencia que pertenece a otro usuario y no es de acceso público.

---

### 2.4. PATCH /api/incidents/{id}/status (Triaje de Operador - RF09, RF12, RF18)
Permite al operador municipal validar o rechazar una incidencia en estado `REPORTED`.

#### Cabeceras
- `Authorization`: `Bearer <token_jwt>` (Rol `OPERATOR` o `ADMIN`)
- `Content-Type`: `application/json`

#### Payload Enviado por el Operador (Request Body)
```json
{
  "targetStatus": "REJECTED",
  "reason": "La incidencia se ubica en término municipal no perteneciente al ayuntamiento."
}
```

#### Parámetros del Formulario, Restricciones y Justificación
| Campo | Tipo | Obligatorio | Valores / Restricciones | Justificación ("Por qué") |
| :--- | :--- | :---: | :--- | :--- |
| `targetStatus` | String | **Sí** | `VALIDATED` \| `REJECTED` | En Sprint 1 solo se admiten las transiciones iniciales desde `REPORTED`. |
| `reason` | String | Condicional | Mín: 10 caracteres<br>Máx: Sin límite (`TEXT`) | Si `targetStatus` es `REJECTED`, es **estrictamente obligatorio** para justificar el rechazo al ciudadano y cumplir la auditoría en `status_changes`. Si es `VALIDATED`, es opcional. |

#### Respuesta del Backend (200 OK)
```json
{
  "id": 101,
  "status": "REJECTED",
  "updatedAt": "2026-10-20T18:45:00Z"
}
```

#### Respuestas de Error
- **`400 Bad Request`**: Falta el motivo obligatorio (`reason`) ante un estado `REJECTED` o el estado solicitado no existe.
- **`409 Conflict`**: Transición no permitida por el ciclo de vida (ejemplo: intentar pasar de `REJECTED` a `VALIDATED` o de `REPORTED` a `CLOSED`).

---

### 2.5. POST /api/incidents/{incidentId}/assets/{assetId}/confirm (Confirmación Manual de Activo Urbano - RF-3, RF-7)
Permite a un operador municipal (`OPERATOR`) o administrador (`ADMIN`) validar y confirmar manualmente la vinculación de un activo urbano candidato o sugerido por proximidad geodésica.

#### Cabeceras
- `Authorization`: `Bearer <token_jwt>` (Rol `OPERATOR` o `ADMIN`)
- `Content-Type`: `application/json`

#### Parámetros de Ruta
| Parámetro | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `incidentId` | Long | **Sí** | Identificador unívoco de la incidencia (`BIGSERIAL`). |
| `assetId` | Long | **Sí** | Identificador unívoco del activo urbano (`BIGSERIAL`). |

#### Payload
No requiere cuerpo en la petición (Request Body vacío).

#### Respuesta del Backend (200 OK)
Devuelve el DTO `IncidentAssetResponse` con el indicador `isConfirmed = true`:
```json
{
  "assetId": 501,
  "assetCode": "SEM-MAL-00142",
  "type": "SEMAFORO",
  "name": "Semáforo Calle Larios cruce Alameda",
  "latitude": 36.71965,
  "longitude": -4.42005,
  "distanceMeters": 6.8,
  "confidenceScore": 0.86,
  "isConfirmed": true
}
```

#### Respuestas de Error
- **`401 Unauthorized`**: Ausencia de token de autenticación o token expirado.
- **`403 Forbidden`**: Petición realizada por un usuario sin permisos suficientes (por ejemplo, rol `CITIZEN`).
- **`404 Not Found`**: La incidencia o la asociación entre la incidencia y el activo no existe en el sistema.

---

### 2.6. DELETE /api/incidents/{incidentId}/assets/{assetId} (Desvinculación de Activo Urbano - RF-4, RF-7)
Permite a un operador municipal (`OPERATOR`) o administrador (`ADMIN`) desvincular y eliminar la asociación de un activo urbano erróneo o irrelevante vinculado a una incidencia.

#### Cabeceras
- `Authorization`: `Bearer <token_jwt>` (Rol `OPERATOR` o `ADMIN`)

#### Parámetros de Ruta
| Parámetro | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `incidentId` | Long | **Sí** | Identificador unívoco de la incidencia (`BIGSERIAL`). |
| `assetId` | Long | **Sí** | Identificador unívoco del activo urbano (`BIGSERIAL`). |

#### Respuesta del Backend (200 OK)
Devuelve `200 OK` (o confirmación de eliminación exitosa):
```json
{
  "message": "Asset association removed successfully",
  "incidentId": 101,
  "assetId": 501
}
```
*(O alternativamente cuerpo vacío con código `200 OK`)*.

#### Respuestas de Error
- **`401 Unauthorized`**: Ausencia de token de autenticación.
- **`403 Forbidden`**: Petición efectuada por rol no operativo (`CITIZEN`).
- **`404 Not Found`**: La incidencia o la asociación de activo indicada no existe.

---

## 3. RESUMEN DE CAMPOS DEVUELTOS POR LA API Y SU SIGNIFICADO

| Campo Devuelto | Tipo | Origen / Cálculo | Por qué se devuelve |
| :--- | :--- | :--- | :--- |
| `id` | Integer / Long | Autoincremental en PostgreSQL (`bigserial`) | Identificador unívoco para tracking y navegación. |
| `status` | String | Estado actual del ciclo de vida | Permite al frontend cambiar botones, colores del mapa y banners informativos. |
| `priority` | String | Por defecto `'MEDIUM'` (`incident_priority`) | Determina el orden de atención en las colas de trabajo. |
| `citizenId` | Integer / Long | Extraído de los claims del token JWT | Garantiza trazabilidad del autor sin exponer sus credenciales ni emails. |
| `district` / `neighborhood` | String / Null | Calculado por cruce espacial o nulo | Permite agrupar incidencias territorialmente. |
| `createdAt` / `updatedAt` | String (ISO-8601) | Marcas temporales de servidor en UTC | Auditoría cronológica de creación y última actualización. |
| `attachments` | Array | Registros asociados en la tabla `attachments` | Listado de URLs de fotos para visualización en carrusel o visor de imágenes. |
| `urbanAssets` | Array | DTO `IncidentAssetResponse` derivado de `incident_assets` | Lista de activos de infraestructura municipal cercanos (<= 50m) o asociados. |
| `urbanContext` | Array | DTO `ExternalObservationResponse` derivado de `incident_context_observations` | Lecturas meteorológicas y de tráfico capturadas al momento de creación. |

---

## 4. MODELOS DE TRANSFERENCIA DE DATOS (DTOS) PARA ACTIVOS Y CONTEXTO EXTERNO

### 4.1. IncidentAssetResponse
Representa los datos de un activo urbano catalogado y la métrica espacial respecto a la incidencia.

```json
{
  "assetId": 501,
  "assetCode": "SEM-MAL-00142",
  "type": "SEMAFORO",
  "name": "Semáforo Calle Larios cruce Alameda",
  "latitude": 36.71965,
  "longitude": -4.42005,
  "distanceMeters": 6.8,
  "confidenceScore": 0.86,
  "isConfirmed": true
}
```

| Atributo | Tipo | Descripción |
| :--- | :--- | :--- |
| `assetId` | Long | ID del activo en el inventario municipal (`urban_assets.id`). |
| `assetCode` | String | Código de referencia municipal (ej. `SEM-MAL-00142`). |
| `type` | String | Tipo o categoría del activo (ej. `SEMAFORO`, `CONTENEDOR`, `FUENTE`, `ALUMBRADO`). |
| `name` | String | Denominación o nombre descriptivo del activo. |
| `latitude` | Double | Latitud geográfica WGS84 del activo. |
| `longitude` | Double | Longitud geográfica WGS84 del activo. |
| `distanceMeters` | Double | Distancia geodésica calculada mediante la fórmula de Haversine en metros. |
| `confidenceScore` | Double | Puntuación de confianza normalizada entre 0.0 y 1.0 (calculada como $1.0 - \frac{d}{50}$). |
| `isConfirmed` | Boolean | `true` si un operador ha validado la relación; `false` si es una sugerencia automática por proximidad. |

### 4.2. ExternalObservationResponse
Representa las observaciones contextuales ambientales y de tráfico obtenidas de fuentes externas en el instante del reporte.

```json
{
  "id": 12,
  "source": "OPEN_METEO",
  "metricType": "TEMPERATURE_C",
  "valueNum": 23.4,
  "valueText": null,
  "observedAt": "2026-10-20T18:30:00Z"
}
```

| Atributo | Tipo | Descripción |
| :--- | :--- | :--- |
| `id` | Long | Identificador de la observación registrada. |
| `source` | String | Fuente externa proveedora (`OPEN_METEO`, `DGT_TRAFFIC`, `MUNICIPAL_SENSORS`). |
| `metricType` | String | Identificador de la métrica (`TEMPERATURE_C`, `PRECIPITATION_MM`, `WIND_SPEED_KMH`, `TRAFFIC_LEVEL`). |
| `valueNum` | Double | Magnitud numérica cuantitativa de la métrica (grados Celsius, mm lluvia, km/h viento). |
| `valueText` | String | Descripción cualitativa o categórica (ej. nivel de tráfico `FLUID`, `HEAVY`). |
| `observedAt` | String (ISO-8601) | Marca temporal de la captura del dato con zona horaria UTC. |
