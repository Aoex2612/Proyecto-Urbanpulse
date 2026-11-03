# Tarea 02: Configuración de Almacenamiento, Spring MVC y Spring Security

**Feature:** `006-adjuntos-multimedia-en-incidencias` (`HU05`)  
**Estado:** Completada ✅  
**Requisitos funcionales y no funcionales:** RNF-Seguridad, RNF-Almacenamiento, RF-3, RF-6  

---

## 1. Resumen de la Tarea

En esta tarea se configuró la infraestructura perimetral del backend para dar soporte a peticiones multipart, almacenamiento físico desacoplado y exposición segura de estáticos:

1. **Propiedades de Configuración Multipart y Directorio:**
   - En `application.properties` y `application-dev.properties`:
     - `app.upload.dir=uploads`
     - `spring.servlet.multipart.max-file-size=5MB`
     - `spring.servlet.multipart.max-request-size=5MB`
   - En `src/test/resources/application.properties`:
     - `app.upload.dir=target/test-uploads` para aislar las pruebas automatizadas y evitar residuos temporales en el entorno de desarrollo.

2. **Configuración de Recursos Estáticos (`WebConfig.java`):**
   - Implementa `WebMvcConfigurer.addResourceHandlers` para mapear el prefijo web `/uploads/**` a la ruta física del sistema de archivos (`file:<normalized-upload-path>/`).
   - Normalización de rutas para interoperabilidad multiplataforma (Linux / Windows).

3. **Seguridad Perimetral (`SecurityConfig.java`):**
   - Configuración de autorización en Spring Security: apertura pública con `.permitAll()` para `/uploads/**`.
   - Permite a los navegadores cliente renderizar las imágenes adjuntas directamente en etiquetas HTML `<img>` sin requerir cabeceras `Authorization: Bearer` en la solicitud del recurso multimedia.
