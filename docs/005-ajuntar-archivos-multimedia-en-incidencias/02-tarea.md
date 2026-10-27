# Tarea 02: Configuración de Almacenamiento, Spring MVC y Spring Security

## Descripción
Configuración de las propiedades multipart, el directorio físico de subida y el servicio de recursos estáticos accesible para clientes web.

## Cambios realizados
- **`application.properties` y `application-dev.properties`**:
  - `app.upload.dir=uploads`
  - `spring.servlet.multipart.max-file-size=5MB`
  - `spring.servlet.multipart.max-request-size=5MB`
- **`application.properties` (test)**:
  - `app.upload.dir=target/test-uploads` para aislar pruebas sin generar residuos en la raíz.
- **`WebConfig.java`**:
  - Implementa `WebMvcConfigurer.addResourceHandlers` para mapear `/uploads/**` a la ruta absoluta normalizada en disco (`file:<path>/`).
- **`SecurityConfig.java`**:
  - Actualización de reglas de autorización perimetral para permitir acceso público a `/uploads/**` sin requerir token JWT, facilitando la visualización directa en etiquetas `<img>`.

## Estado
- **Completada**: El servidor expone `/uploads/**` y soporta peticiones multipart de hasta 5MB.
