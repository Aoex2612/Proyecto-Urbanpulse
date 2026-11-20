# Documentación de Tarea 7: Componentes Frontend para Activos Urbanos y Contexto Externo

## Descripción
En esta tarea se ha desarrollado la interfaz de usuario para la visualización y gestión reactiva de los activos urbanos vinculados (`UrbanAsset`) y las observaciones ambientales y de tráfico municipal (`UrbanContext`) asociadas a las incidencias urbanas en UrbanPulse.

Cubre los requisitos funcionales:
- **RF-3**: Confirmación de asociaciones de activos por parte de operadores/administradores (`POST /api/incidents/{incidentId}/assets/{assetId}/confirm`).
- **RF-4**: Desvinculación de asociaciones de activos por parte de operadores/administradores (`DELETE /api/incidents/{incidentId}/assets/{assetId}`).
- **RF-5**: Presentación detallada en la ficha de la incidencia de los activos vinculados con su código, tipo, denominación, distancia geodésica calculada en metros, porcentaje de confianza y flag de confirmación.
- **RF-10**: Presentación en la ficha de la incidencia del bloque de contexto externo con lecturas meteorológicas (temperatura en °C, precipitación en mm, velocidad del viento en km/h) y estado del tráfico municipal capturados al reportarse la incidencia.

---

## Archivos Creados y Modificados

### 1. Cliente API (`frontend/src/services/incidentService.js`)
Se agregaron las funciones cliente para la interacción con los nuevos endpoints REST del backend:
- `confirmIncidentAsset(incidentId, assetId)`: Realiza una petición `POST /api/incidents/${incidentId}/assets/${assetId}/confirm`. Retorna el objeto `IncidentAssetResponse` con `isConfirmed = true`.
- `removeIncidentAsset(incidentId, assetId)`: Realiza una petición `DELETE /api/incidents/${incidentId}/assets/${assetId}`.

### 2. Componente `IncidentAssetCard.jsx` y Estilos (`IncidentAssetCard.css`)
- **Ruta:** `frontend/src/components/Map/IncidentAssetCard.jsx`
- **Funcionalidad:**
  - Renderiza el catálogo de activos urbanos próximos dentro del radio de 50 metros.
  - Muestra badges informativos para el código municipal, tipología del activo, distancia formateada en metros y porcentaje de confianza probabilística ($1.0 - d/50$).
  - Distingue visualmente mediante etiquetas los activos con estado `✓ Confirmado` frente a los candidatos automáticos en estado `Sugerido`.
  - Presenta botones de acción ("Confirmar" y "Desvincular") exclusivamente cuando `canManage = true` (usuarios con rol `OPERATOR` o `ADMIN`).
  - Proporciona retroalimentación reactiva inmediata (mensajes de éxito o error) e inhabilita los botones mientras la petición está en curso (`processingId`).
  - Renderiza un estado vacío descriptivo si no hay activos detectados en las inmediaciones.

### 3. Componente `IncidentContextCard.jsx` y Estilos (`IncidentContextCard.css`)
- **Ruta:** `frontend/src/components/Map/IncidentContextCard.jsx`
- **Funcionalidad:**
  - Renderiza una rejilla estructurada de tarjetas con las métricas contextuales recogidas en el momento del aviso.
  - Filtra y formatea métricas específicas:
    - **Temperatura:** En grados Celsius con precisión decimal (`°C`).
    - **Precipitación / Lluvia:** En milímetros (`mm`).
    - **Viento:** Velocidad en kilómetros por hora (`km/h`).
    - **Tráfico:** Nivel de fluidez o congestión vial en el tramo.
  - Despliega la marca temporal del instante de captura de la observación.
  - Soporta degradación controlada: si no existen observaciones (por fallback o indisponibilidad de APIs externas), renderiza un mensaje informativo sin romper la interfaz.

### 4. Integración en `IncidentDetailModal.jsx` y Barril (`index.js`)
- En `frontend/src/components/Map/IncidentDetailModal.jsx`:
  - Se evalúa si el usuario autenticado tiene permisos operativos (`canManageAssets = effectiveUser?.role === 'OPERATOR' || effectiveUser?.role === 'ADMIN'`).
  - Se incorporan `IncidentAssetCard` e `IncidentContextCard` en el cuerpo del modal de detalle de la incidencia, alimentados con `incident.urbanAssets` e `incident.urbanContext`.
- En `frontend/src/components/Map/index.js`:
  - Se exportan `IncidentAssetCard` e `IncidentContextCard` para facilitar su reutilización en otros módulos y vistas (como el panel de triaje de operador).

### 5. Pruebas Unitarias en Vitest
- **`frontend/src/components/Map/IncidentAssetCard.test.jsx`**:
  - Renderizado de lista vacía con mensaje informativo.
  - Renderizado de activos con datos de distancia, tipo, código y confianza.
  - Ocultación de controles de confirmación/desvinculación para ciudadanos (`canManage=false`).
  - Visibilidad de botones y badge de gestión para operadores (`canManage=true`).
  - Flujo de confirmación exitosa con invocación de servicio y mensaje de feedback.
  - Flujo de desvinculación exitosa con feedback.
  - Tratamiento y despliegue de mensajes de error de la API.
- **`frontend/src/components/Map/IncidentContextCard.test.jsx`**:
  - Renderizado de mensaje de degradación cuando no hay observaciones.
  - Renderizado correcto de métricas de temperatura, precipitación, viento y tráfico.
  - Despliegue de marca temporal de captura.
  - Manejo de observaciones parciales con valores por defecto.

---

## Verificación y Calidad
- Código modular documentado exhaustivamente con JSDoc en todas las funciones y componentes.
- Accesibilidad ARIA incluida en secciones, encabezados, estados (`role="status"`) y botones etiquetados con `aria-label`.
- Diseño responsivo adaptado para dispositivos móviles y escritorio.
