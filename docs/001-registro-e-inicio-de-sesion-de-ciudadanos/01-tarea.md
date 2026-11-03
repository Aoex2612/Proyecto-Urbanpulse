# Tarea 1: Configurar dependencias, seguridad y CORS en Spring Boot

**Feature:** `001-registro-e-inicio-de-sesion-de-ciudadanos`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-6, RF-7  

---

## 1. Resumen de la Tarea

Se ha configurado la infraestructura base del backend en Spring Boot para permitir el desarrollo seguro y desacoplado del sistema de autenticación de UrbanPulse. Se incorporaron todas las dependencias necesarias para persistencia, seguridad y tokens JWT, se definieron los parámetros de entorno en `application.properties`, y se implementó la configuración de seguridad con arquitectura sin estado (stateless), exclusión de CSRF para API REST, apertura de rutas `/api/auth/**`, bean codificador de contraseñas con BCrypt y reglas CORS orientadas al cliente de desarrollo en Vite/React.

---

## 2. Subtareas implementadas

### 2.1 Dependencias en `Backend/pom.xml`
- Configurado `spring-boot-starter-parent` en versión **3.4.3** y compatibilidad con Java 17.
- Incluidos starters requeridos:
  - `spring-boot-starter-web`: Soporte REST y servidor embebido.
  - `spring-boot-starter-data-jpa`: Capa de persistencia ORM con Hibernate.
  - `spring-boot-starter-security`: Infraestructura de seguridad y filtros de autenticación/autorización.
  - `spring-boot-starter-validation`: Soporte para Bean Validation con anotaciones estándar (`@NotBlank`, `@Email`, `@Size`, etc.).
  - `postgresql`: Driver JDBC para conexión con la base de datos PostgreSQL.
  - `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (versión `0.12.6`): Librería para generación, firma y parseo de tokens JWT.
  - `spring-boot-starter-test` y `spring-security-test`: Entorno para pruebas unitarias y de integración con JUnit 5.
  - `spring-boot-maven-plugin`: Plugin para empaquetado y ejecución.

### 2.2 Configuración de la aplicación en `Backend/src/main/resources/application.properties`
- Configurada conexión JDBC hacia `urban_pulse_db`:
  - `spring.datasource.url=jdbc:postgresql://localhost:5432/urban_pulse_db`
  - `spring.datasource.username=admin`
  - `spring.datasource.password=database`
  - `spring.datasource.driver-class-name=org.postgresql.Driver`
- Propiedades JPA / Hibernate (`hibernate.ddl-auto=update`, dialecto PostgreSQL).
- Parámetros JWT:
  - `jwt.secret`: Clave secreta criptográfica de 512 bits en hexadecimal (>256 bits requeridos).
  - `jwt.expiration`: 86400000 ms (24 horas).

### 2.3 Clase de seguridad en `Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java`
- Configurada cadena de filtros de seguridad HTTP (`SecurityFilterChain`):
  - Arquitectura sin estado (`SessionCreationPolicy.STATELESS`).
  - Protección CSRF desactivada (`AbstractHttpConfigurer::disable`) adecuada para APIs REST sin sesión de servidor.
  - Endpoints públicos bajo `/api/auth/**` con `permitAll()`.
  - Resto de endpoints protegidos mediante `authenticated()`.
- Bean `PasswordEncoder`:
  - Provee una instancia de `BCryptPasswordEncoder` para el hashing de contraseñas.
- Todo el código Java rigurosamente documentado con bloques Javadoc (clase, métodos, `@param`, `@return`, `@throws`).

### 2.4 Configuración de CORS en Spring Security
- Configurado bean `CorsConfigurationSource`:
  - Origen permitido: `http://localhost:5173` (cliente de desarrollo Vite/React).
  - Métodos HTTP permitidos: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`.
  - Cabeceras autorizadas: `Authorization`, `Content-Type`.
  - `allowCredentials` habilitado (`true`).
  - Patrón aplicado: `/**`.

### 2.5 Verificación y pruebas
- Actualizada clase principal `Backend/src/main/java/org/bulbaseros/App.java` con `@SpringBootApplication` y Javadoc completo.
- Creado `Backend/src/test/resources/application.properties` para configuración de testing ligero.
- Implementado `Backend/src/test/java/org/bulbaseros/config/SecurityConfigTest.java` verificando:
  - Codificación y validación de coincidencia de contraseñas con BCrypt.
  - Registro de configuraciones CORS (origen, métodos y cabeceras).
- Actualizado `Backend/src/test/java/org/bulbaseros/AppTest.java` con JUnit 5.
- Ejecución limpia de Maven: `mvn clean test` (3 tests ejecutados, 0 fallos, 0 errores).

---

## 3. Archivos creados / modificados

| Archivo | Tipo de cambio | Descripción |
|---|---|---|
| `Backend/pom.xml` | Modificado | Declaración de dependencias Spring Boot 3.4.3, JWT, PostgreSQL y Testing |
| `Backend/src/main/resources/application.properties` | Creado | Configuración de datasource, JPA y propiedades JWT |
| `Backend/src/main/java/org/bulbaseros/App.java` | Modificado | Anotación `@SpringBootApplication` y documentación Javadoc |
| `Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java` | Creado | Configuración de Spring Security, CORS y PasswordEncoder |
| `Backend/src/test/resources/application.properties` | Creado | Configuración de propiedades para ejecución de pruebas unitarias |
| `Backend/src/test/java/org/bulbaseros/AppTest.java` | Modificado | Actualización a JUnit 5 y documentación Javadoc |
| `Backend/src/test/java/org/bulbaseros/config/SecurityConfigTest.java` | Creado | Pruebas unitarias para BCrypt y CORS |
| `features/001-registro-e-inicio-de-sesion-de-ciudadanos/tasks.md` | Modificado | Marcado de Tarea 1 y subtareas como completadas `[x]` |
| `docs/001-registro-e-inicio-de-sesion-de-ciudadanos/01-tarea.md` | Creado | Documentación de la tarea realizada |

---

## 4. Resultado de las Pruebas

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.bulbaseros.AppTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.027 s -- in org.bulbaseros.AppTest
[INFO] Running org.bulbaseros.config.SecurityConfigTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.180 s -- in org.bulbaseros.config.SecurityConfigTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
