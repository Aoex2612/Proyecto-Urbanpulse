# ADR-04: Cartografía interactiva con Leaflet y OpenStreetMap frente a servicios privativos

* Estado: aceptado
* Fecha: 2026-10-26

## Contexto
El núcleo de valor de UrbanPulse reside en la monitorización y reporte geolocalizado de incidencias y activos urbanos municipales. La interfaz web requería integrar un mapa interactivo con soporte para:
1. Renderizado fluido de marcadores geolocalizados de incidencias con información contextual (código, título, estado, prioridad).
2. Selección interactiva de coordenadas (latitud y longitud) mediante clics sobre el mapa para nuevos reportes ciudadanos.
3. Compatibilidad con el ecosistema React 19 y empaquetador Vite.
4. Evitar costes por consumo de APIs propietarias o requisitos obligatorios de tarjetas de crédito/facturación para desarrollo y entornos comunitarios.

## Opciones consideradas
* **Opción 1: Google Maps JavaScript API:** Proveedor líder en cobertura y cartografía comercial.
* **Opción 2: Mapbox GL JS:** Solución basada en WebGL con excelente renderizado de vectores y mapas personalizados.
* **Opción 3: Leaflet y React-Leaflet con teselas de OpenStreetMap (OSM):** Librería JavaScript ligera, de código abierto, con amplia comunidad y capas de teselas abiertas y gratuitas.

## Decisión tomada
Optamos por la **Opción 3: Leaflet 1.9.x y React-Leaflet con teselas estándar de OpenStreetMap**, porque proporciona todas las capacidades cartográficas requeridas para la gestión urbana municipal bajo un modelo de código abierto, sin coste por consumo de API ni dependencias de claves propietarias de facturación.

### Razón / Por qué se tomó la decisión
* **Independencia de proveedores y costes cero:** A diferencia de Google Maps o Mapbox, el uso de OpenStreetMap y Leaflet no impone cuotas de facturación ni requiere gestionar API keys restringidas en el frontend para funcionar en desarrollo, pruebas y despliegues estándar.
* **Ligereza y rendimiento:** Leaflet es sumamente ligero (< 40 KB de JS base), cargando rápido en conexiones móviles o navegadores con recursos limitados.
* **Integración declarativa con React:** La biblioteca `react-leaflet` se acopla de manera natural al ciclo de vida de componentes React, facilitando la creación de componentes modulares (`MapContainer`, `TileLayer`, `Marker`, `Popup`).
* **Suficiencia funcional:** Cubre con creces las necesidades de posicionamiento por coordenadas WGS84 (latitud/longitud decimales), popups informativos y eventos de clic.

## Consecuencias
* **Positivo:** Sin costes recurrentes de API, sin riesgos de bloqueo por cuotas mensuales, integración limpia en la SPA y desarrollo local inmediato sin configuración de credenciales externas.
* **Negativo:** El servidor de teselas estándar de OpenStreetMap (`tile.openstreetmap.org`) tiene políticas de uso aceptable estrictas que desaconsejan tráfico masivo en producción intensiva; requerirá configurar un proveedor de teselas comercial (como Stadia Maps o MapTiler) o un servidor de teselas propio en caso de despliegue a gran escala municipal.
* **Neutral:** El renderizado es basado en imágenes raster en lugar de renderizado vectorial por GPU (WebGL), lo cual es suficiente para la escala urbana del proyecto.
