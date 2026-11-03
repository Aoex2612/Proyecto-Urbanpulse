# Tarea 2: Entidad de Dominio y Repositorio de Usuario

**Feature:** `001-registro-e-inicio-de-sesion-de-ciudadanos`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-2  

---

## 1. Resumen de la Tarea

Se ha implementado el modelo de dominio y la capa de persistencia básica para los usuarios del sistema UrbanPulse. Se definió el enumerado `UserRole` representando los roles válidos según el esquema relacional de PostgreSQL (`user_role`), la entidad JPA `User` vinculada a la tabla `users` con validaciones de longitud, protección de datos sensibles en su representación textual y asignación por defecto del rol `CITIZEN`, y el repositorio Spring Data JPA `UserRepository` con operaciones de verificación de existencia y búsqueda por correo electrónico.

Asimismo, se configuró la dependencia de base de datos en memoria H2 (`<scope>test</scope>`) para la ejecución aislada y reproducible de las pruebas de integración JPA, implementando suites exhaustivas de pruebas unitarias y de repositorio.

---

## 2. Subtareas implementadas

### 2.1 Enumerado `UserRole` (`Backend/src/main/java/org/bulbaseros/model/UserRole.java`)
- Mapeo directo del tipo enumerado `user_role` definido en el esquema inicial de PostgreSQL:
  - `CITIZEN`: Ciudadano registrado para reporte y seguimiento de incidencias urbanas.
  - `OPERATOR`: Operador del centro de control municipal para triaje y validación inicial.
  - `TECHNICIAN`: Técnico de cuadrilla asignado a la resolución técnica de incidencias.
  - `ADMIN`: Administrador con privilegios globales.
  - `ANALYST`: Analista para monitorización y métricas urbanas.
- Documentado con Javadoc exhaustivo a nivel de tipo y constantes.

### 2.2 Entidad `User` (`Backend/src/main/java/org/bulbaseros/model/User.java`)
- Anotada con `@Entity` y vinculada a la tabla `users`.
- Clave primaria `id` mapeada con `@Id` y `@GeneratedValue(strategy = GenerationType.IDENTITY)` compatible con `BIGSERIAL`.
- Atributo `email` mapeado a columna `email` con restricción de unicidad (`unique = true`), obligatoriedad (`nullable = false`), longitud máxima de 150 caracteres y validación Bean Validation `@Email` y `@Size(max = 150)`.
- Atributo `passwordHash` mapeado a columna `password_hash` (`@Column(name = "password_hash")`), obligatorio, longitud máxima de 255 caracteres para almacenar el hash BCrypt.
- Atributo `fullName` mapeado a columna `full_name`, obligatorio, longitud máxima de 150 caracteres.
- Atributo `role` mapeado con `@Enumerated(EnumType.STRING)` y `@Column(name = "role", nullable = false)`. Por defecto se inicializa en `UserRole.CITIZEN`.
- Atributo `departmentId` mapeado a columna `department_id` como `Long` nullable para permitir la asignación opcional de departamentos municipales.
- Atributo `createdAt` mapeado con `@CreationTimestamp` y `@Column(name = "created_at", updatable = false)` con tipo `OffsetDateTime` (alineado con `timestamptz`).
- Métodos constructores (por defecto para JPA, constructor de registro para ciudadano, constructor con rol y constructor completo).
- Validaciones defensivas en métodos setters (`setEmail`, `setFullName`, `setPasswordHash`) que rechazan cadenas que superen las longitudes del esquema con `IllegalArgumentException`.
- Implementación de `equals` y `hashCode` coherente con entidades JPA.
- Sobrescritura de `toString()` excluyendo estrictamente `passwordHash` para evitar filtraciones involuntarias en logs de depuración.
- Documentación completa mediante bloques Javadoc en todos los miembros.

### 2.3 Repositorio `UserRepository` (`Backend/src/main/java/org/bulbaseros/repository/UserRepository.java`)
- Interfaz anotada con `@Repository` que extiende `JpaRepository<User, Long>`.
- Métodos de consulta derivados de Spring Data:
  - `boolean existsByEmail(String email);`: Comprueba la existencia previa de un correo para garantizar unicidad en el registro (RF-2).
  - `Optional<User> findByEmail(String email);`: Recupera la entidad por email para la autenticación durante el inicio de sesión (RF-1).
- Documentación Javadoc con especificación de `@param` y `@return`.

### 2.4 Configuración de pruebas y verificación
- Incorporada la dependencia `com.h2database:h2` con `<scope>test</scope>` en `Backend/pom.xml` para pruebas automatizadas sin depender de un contenedor PostgreSQL externo activo en CI/entornos locales.
- Actualizado `Backend/src/test/resources/application.properties` con configuración en memoria compatible con PostgreSQL (`MODE=PostgreSQL`) y estrategia DDL `create-drop`.
- Creada suite unitaria `Backend/src/test/java/org/bulbaseros/model/UserTest.java` (10 pruebas) validando:
  - Valores por defecto y rol `CITIZEN`.
  - Sobrecarga de constructores y fallback a `CITIZEN` ante rol nulo.
  - Funcionamiento de getters y setters.
  - Rechazo de longitudes superiores a 150 caracteres para email y nombre, y 255 para hash.
  - Ocultación del hash en `toString()`.
  - Contratos de igualdad y consistencia de hash code.
- Creada suite de persistencia `Backend/src/test/java/org/bulbaseros/repository/UserRepositoryTest.java` (5 pruebas) con `@DataJpaTest` validando:
  - Inserción y recuperación mediante `findByEmail`.
  - Retorno de `Optional.empty()` para correos inexistentes.
  - Verificación de existencia positiva y negativa con `existsByEmail`.
  - Persistencia de usuarios con rol `TECHNICIAN` y departamento.
  - Auto-generación de `id` y `createdAt`.
- Ejecución limpia de Maven (`mvn clean test`): 18 pruebas ejecutadas, 0 fallos, 0 errores.

---

## 3. Archivos creados / modificados

| Archivo | Tipo de cambio | Descripción |
|---|---|---|
| `Backend/src/main/java/org/bulbaseros/model/UserRole.java` | Creado | Enumerado de roles del sistema (`CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`) |
| `Backend/src/main/java/org/bulbaseros/model/User.java` | Creado | Entidad JPA mapeada a la tabla `users` con anotaciones, validaciones y Javadoc |
| `Backend/src/main/java/org/bulbaseros/repository/UserRepository.java` | Creado | Interfaz Spring Data JPA con `existsByEmail` y `findByEmail` |
| `Backend/pom.xml` | Modificado | Declaración de H2 Database en `<scope>test</scope>` para pruebas de integración JPA |
| `Backend/src/test/resources/application.properties` | Modificado | Configuración de datasource H2 en memoria para pruebas automáticas |
| `Backend/src/test/java/org/bulbaseros/model/UserTest.java` | Creado | Pruebas unitarias de la entidad `User` (10 pruebas) |
| `Backend/src/test/java/org/bulbaseros/repository/UserRepositoryTest.java` | Creado | Pruebas `@DataJpaTest` para `UserRepository` (5 pruebas) |
| `features/001-registro-e-inicio-de-sesion-de-ciudadanos/tasks.md` | Modificado | Marcado de Tarea 2 y subtareas como completadas `[x]` |
| `docs/001-registro-e-inicio-de-sesion-de-ciudadanos/02-tarea.md` | Creado | Documentación detallada de la Tarea 2 |

---

## 4. Resultado de las Pruebas

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.bulbaseros.AppTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.026 s -- in org.bulbaseros.AppTest
[INFO] Running org.bulbaseros.config.SecurityConfigTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.191 s -- in org.bulbaseros.config.SecurityConfigTest
[INFO] Running org.bulbaseros.model.UserTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.018 s -- in org.bulbaseros.model.UserTest
[INFO] Running org.bulbaseros.repository.UserRepositoryTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.267 s -- in org.bulbaseros.repository.UserRepositoryTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.873 s
[INFO] Finished at: 2026-10-20T21:02:01+02:00
[INFO] ------------------------------------------------------------------------
```
