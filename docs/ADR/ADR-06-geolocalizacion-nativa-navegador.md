# ADR-06: Captura de ubicación con API nativa W3C Geolocation en el cliente

* Estado: aceptado
* Fecha: 2026-10-28

## Contexto
Durante el proceso de reporte de incidencias urbanas en UrbanPulse, los usuarios que se encuentran presencialmente en el lugar del desperfecto necesitan fijar sus coordenadas geográficas con rapidez y máxima precisión, sin depender obligatoriamente de la búsqueda y pulsación manual sobre el mapa interactivo. La solución requiere:
1. Obtener la latitud y longitud con precisión submétrica (dispositivos móviles y portátiles con GPS o triangulación Wi-Fi/celular).
2. Respetar la privacidad del ciudadano solicitando consentimiento explícito únicamente bajo demanda (clic voluntario).
3. No incurrir en dependencias de librerías de terceros pesadas ni en servicios externos de pago o con límites de cuota.
4. Funcionar de forma desacoplada dentro del ciclo de vida de React y de los componentes de alta de incidencias.

## Opciones consideradas
* **Opción 1: API nativa del navegador W3C `navigator.geolocation`:** Estándar nativo soportado por todos los navegadores modernos, que accede directamente al hardware GPS o fuentes de geolocalización del sistema operativo con soporte para `enableHighAccuracy`.
* **Opción 2: Geolocalización por dirección IP mediante servicios externos (e.g. ipapi, ipstack):** Resolución de coordenadas a partir de la IP pública del cliente mediante peticiones HTTP a servidores de terceros.
* **Opción 3: Plugin externo de Leaflet (`leaflet.locatecontrol`):** Paquete adicional que añade un control flotante directamente superpuesto al lienzo del mapa de Leaflet.

## Decisión tomada
Optamos por la **Opción 1: API nativa W3C `navigator.geolocation` encapsulada en un módulo de utilidad (`geolocationUtils.js`)**, porque ofrece la máxima precisión real del dispositivo (GPS/Wi-Fi) con coste computacional y de red cero, sin dependencias externas en el empaquetado y respetando los estándares de seguridad de contextos seguros (HTTPS).

### Razón / Por qué se tomó la decisión
* **Precisión real para incidencias en vía pública:** Las incidencias urbanas (baches, luminarias, mobiliario roto) requieren precisión a nivel de calle o acera (submétrica o de pocos metros). Los servicios basados en IP (Opción 2) devuelven estimaciones a nivel de ciudad o centralita telefónica (con márgenes de error de varios kilómetros), resultando completamente inútiles para la gestión municipal.
* **Sin sobrecarga de dependencias ni acoplamiento:** A diferencia del plugin de Leaflet (Opción 3), la API nativa de geolocalización permite desacoplar la lógica de captura geográfica del lienzo de Leaflet, integrándose limpiamente en el formulario modal de alta y facilitando su testeo mediante mocks estándar de `navigator.geolocation`.
* **Seguridad y privacidad por diseño:** El navegador solicita el permiso de forma nativa e inmutable, impidiendo accesos no consentidos. Se configura con `enableHighAccuracy: true`, `timeout: 10000` y `maximumAge: 0` para asegurar que las coordenadas no provengan de cachés obsoletas.

## Consecuencias
* **Positivo:** Precisión óptima en dispositivos móviles y portátiles; 0 bytes adicionales de dependencias de terceros; compatibilidad total con navegadores modernos; control granular de estados de carga, permisos denegados y timeouts.
* **Negativo:** En navegadores de escritorio sin GPS o conexiones de red cableada sin geolocalización asistida, la precisión puede ser inferior a la de un dispositivo móvil (mitigado permitiendo al usuario corregir o mantener el punto fijado en el mapa).
* **Neutral:** Requiere que la aplicación se sirva bajo contexto seguro (HTTPS en producción o `localhost` en desarrollo) para que los navegadores autoricen el acceso a la API.
