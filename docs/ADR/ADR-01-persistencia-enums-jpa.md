# ADR-01: Uso de VARCHAR con restricciones CHECK para persistencia de tipos enumerados con JPA

* Estado: aceptado
* Fecha: 2026-10-23

## Contexto
El modelo de datos inicial del sistema UrbanPulse definía tipos enumerados nativos en PostgreSQL mediante sentencias `CREATE TYPE ... AS ENUM` para los atributos funcionales `user_role`, `incident_status` e `incident_priority`.

Al desarrollar la capa de persistencia en el backend mediante Spring Data JPA e Hibernate (utilizando la anotación estándar `@Enumerated(EnumType.STRING)`), se identificó una incompatibilidad técnica:
* Hibernate espera columnas relacionales de tipo estándar `VARCHAR` al persistir enumeraciones como cadenas.
* Los tipos `ENUM` nativos de PostgreSQL requieren conversores personalizados o tipos específicos del driver JDBC (como `PGobject` o dependencias de terceros como Hypersistence/Hibernate-types).
* En entornos de desarrollo y pruebas automatizadas que emplean bases de datos en memoria (como H2 en modo compatibilidad PostgreSQL), los tipos `ENUM` nativos de PostgreSQL no son interpretados de manera homogénea, provocando fallos de validación del esquema con `ddl-auto=validate` o `ddl-auto=update`.

## Opciones consideradas
* **Opción 1:** Mantener `CREATE TYPE ... AS ENUM` en PostgreSQL y configurar tipos personalizados en Hibernate mediante clases `UserType` o `@Type` propietarias.
* **Opción 2:** Sustituir los `ENUM` nativos por columnas `VARCHAR` con restricciones de integridad de dominio `CHECK` a nivel de base de datos.
* **Opción 3:** Almacenar las enumeraciones como enteros secuenciales (`EnumType.ORDINAL`).

## Decisión tomada
Optamos por la **Opción 2: columnas `VARCHAR` con restricciones `CHECK`** (`VARCHAR(30) CHECK ("role" IN (...))`), porque garantiza compatibilidad directa y estándar con JPA `@Enumerated(EnumType.STRING)` tanto en PostgreSQL 16 como en la base de datos de pruebas H2, manteniendo la integridad referencial y de dominio en el motor de base de datos sin introducir dependencias propietarias en el código Java.

### Razón / Por qué se tomó la decisión
* **Estandarización y simplicidad:** Permite utilizar directamente `@Enumerated(EnumType.STRING)` en las entidades JPA (`Usuario`, `Incidencia`) sin configuraciones de dialecto adicionales.
* **Portabilidad entre entornos:** Facilita la ejecución de pruebas unitarias y de integración contra H2 en memoria y PostgreSQL en Docker de manera idéntica.
* **Integridad de datos preservada:** La restricción `CHECK` a nivel de tabla en PostgreSQL rechaza cualquier valor fuera del catálogo permitido, garantizando la misma seguridad semántica que un tipo `ENUM` nativo.
* **Legibilidad:** En consultas SQL directas y volcados de base de datos, los valores se leen de forma explícita (ej. `'OPERATOR'`, `'CITIZEN'`) frente al riesgo de desincronización de índices de `EnumType.ORDINAL`.

## Consecuencias
* **Positivo:** Eliminación de errores de arranque en Hibernate con `ddl-auto=update` / `validate`, compatibilidad directa con los 116 tests unitarios existentes y facilidad de despliegue en Docker.
* **Negativo:** En PostgreSQL un `VARCHAR` ocupa ligeramente más espacio en disco que un `ENUM` interno de 4 bytes, aunque con el volumen esperado de registros del dominio este impacto es insignificante.
* **Neutral:** Si se añade un nuevo valor a un enum en el código Java (por ejemplo, un nuevo rol en `RolUsuario`), es necesario actualizar la restricción `CHECK` del script SQL de inicialización.
