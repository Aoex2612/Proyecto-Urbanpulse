# Tarea 3: Proveedor y Servicio de Tokens JWT

**Feature:** `001-registro-e-inicio-de-sesion-de-ciudadanos`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-6, RF-9  

---

## 1. Resumen de la Tarea

Se ha implementado el servicio integral de emisión, parseo y validación criptográfica de tokens JWT (`JwtService`) para la plataforma UrbanPulse. La implementación utiliza la biblioteca JJWT (versión 0.12.6) con el estándar de firma HMAC-SHA256, garantizando una arquitectura de autenticación completamente desacoplada y sin estado (stateless). El servicio maneja claims estándar como el sujeto (`sub` asociado al correo electrónico del usuario), marca temporal de emisión (`iat`) y fecha de expiración (`exp`), junto a claims personalizados como el rol funcional (`role`). Asimismo, se han creado pruebas unitarias rigurosas que validan cada aspecto del ciclo de vida del token.

---

## 2. Subtareas implementadas

### 2.1 Implementación de `JwtService` (`Backend/src/main/java/org/bulbaseros/security/JwtService.java`)
- Anotado como componente `@Service` gestionado por Spring.
- Inyección de propiedades mediante `@Value`:
  - `jwt.secret`: Clave secreta criptográfica (definida con >256 bits en `application.properties`).
  - `jwt.expiration`: Tiempo de validez del token en milisegundos (86.400.000 ms, equivalente a 24 horas).
- Constructores:
  - Constructor por defecto para inyección de dependencias en contexto Spring.
  - Constructor parametrizado `(String secretKey, long jwtExpiration)` para permitir instanciación directa y pruebas unitarias aisladas.
- Métodos de generación de tokens:
  - `generateToken(User user)`: Emite un token a partir de la entidad `User`, asignando `user.getEmail()` como subject y `user.getRole().name()` como claim `role`.
  - `generateToken(String email, String role)`: Sobrecarga para emitir tokens a partir de credenciales y roles textuales.
  - `generateToken(Map<String, Object> extraClaims, String email)`: Método base que construye el payload JWT con los claims suministrados, las fechas de emisión/expiración y la firma criptográfica HMAC-SHA256 generada con `Keys.hmacShaKeyFor(...)`.
- Método de clave de firma (`getSigningKey`):
  - Algoritmo de resolución flexible: evalúa decodificación Base64 y, ante cadenas de texto plano o hex con caracteres especiales, utiliza la representación en bytes UTF-8 asegurando el cumplimiento del mínimo de 256 bits para HMAC-SHA256.

### 2.2 Extracción de claims y validaciones criptográficas
- `extractEmail(String token)`: Resuelve y retorna el sujeto (`sub`) del token.
- `extractRole(String token)`: Resuelve y retorna el claim personalizado `role`.
- `extractExpiration(String token)`: Retorna la fecha de expiración (`exp`) como `java.util.Date`.
- `extractClaim(String token, Function<Claims, T> claimsResolver)`: Método genérico para extracción extensible de atributos del payload.
- `extractAllClaims(String token)`: Parsea y valida criptográficamente el token con el parser de JJWT (`Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`).
- `isTokenExpired(String token)`: Comprueba si el instante actual es posterior a la fecha de caducidad del token, capturando excepciones de parseo.
- `isTokenValid(String token, String email)`: Realiza una validación combinada de firma legítima, no expiración y coincidencia estricta del sujeto con el correo electrónico del usuario.

### 2.3 Pruebas Unitarias (`Backend/src/test/java/org/bulbaseros/security/JwtServiceTest.java`)
Se han implementado 8 pruebas unitarias exhaustivas con JUnit 5:
1. `shouldGenerateTokenFromUserAndExtractClaims`: Comprueba la generación de token a partir de `User` y la posterior extracción fiel del email y del rol `CITIZEN`.
2. `shouldGenerateTokenFromEmailAndRole`: Valida la emisión mediante sobrecarga con argumentos explícitos (`operador@urbanpulse.es`, `OPERATOR`).
3. `shouldValidateLegitimateToken`: Verifica que `isTokenValid` devuelva `true` para el propietario y `false` para un correo diferente.
4. `shouldCalculateExpirationAround24Hours`: Comprueba que la expiración se calcule a 24 horas (~86.400.000 ms) y que un token recién generado no figure expirado.
5. `shouldDetectExpiredTokens`: Verifica con un servicio de expiración negativa (-1000 ms) que los tokens expirados se detecten como caducados e inválidos.
6. `shouldRejectTokenWithInvalidSignature`: Valida que tokens firmados con una clave secreta distinta sean rechazados por discrepancia de firma (`isTokenValid` retorna `false` y `extractAllClaims` lanza `JwtException`).
7. `shouldRejectMalformedTokens`: Verifica el rechazo seguro de cadenas corruptas o con formato no JWT.
8. `shouldThrowExceptionForInvalidArguments`: Comprueba que se lancen excepciones `IllegalArgumentException` ante argumentos nulos o cadenas vacías.

---

## 3. Archivos creados / modificados

| Archivo | Tipo de cambio | Descripción |
|---|---|---|
| `Backend/src/main/java/org/bulbaseros/security/JwtService.java` | Creado | Servicio Spring para generación, firma, extracción y verificación de tokens JWT con JJWT |
| `Backend/src/test/java/org/bulbaseros/security/JwtServiceTest.java` | Creado | Suite de pruebas unitarias cubriendo generación, claims, expiración y firma |
| `features/001-registro-e-inicio-de-sesion-de-ciudadanos/tasks.md` | Modificado | Marcado de Tarea 3 y subtareas 3.1 y 3.2 como completadas `[x]` |
| `docs/001-registro-e-inicio-de-sesion-de-ciudadanos/03-tarea.md` | Creado | Documentación técnica de la tarea realizada |

---

## 4. Resultado de la Verificación y Tests

Ejecución de la suite completa de pruebas mediante `cd Backend && mvn clean test`:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.bulbaseros.AppTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.026 s -- in org.bulbaseros.AppTest
[INFO] Running org.bulbaseros.config.SecurityConfigTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.183 s -- in org.bulbaseros.config.SecurityConfigTest
[INFO] Running org.bulbaseros.dto.ValidationDtoTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.190 s -- in org.bulbaseros.dto.ValidationDtoTest
[INFO] Running org.bulbaseros.exception.GlobalExceptionHandlerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in org.bulbaseros.exception.GlobalExceptionHandlerTest
[INFO] Running org.bulbaseros.model.UserTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in org.bulbaseros.model.UserTest
[INFO] Running org.bulbaseros.repository.UserRepositoryTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.288 s -- in org.bulbaseros.repository.UserRepositoryTest
[INFO] Running org.bulbaseros.security.JwtServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.131 s -- in org.bulbaseros.security.JwtServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  5.402 s
[INFO] Finished at: 2026-10-20T21:05:54+02:00
[INFO] ------------------------------------------------------------------------
```
