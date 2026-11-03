package org.bulbaseros.controller;

import jakarta.validation.Valid;
import org.bulbaseros.dto.AuthResponse;
import org.bulbaseros.dto.LoginRequest;
import org.bulbaseros.dto.RegisterRequest;
import org.bulbaseros.dto.RegisterResponse;
import org.bulbaseros.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST que expone los endpoints de autenticación y registro de usuarios en UrbanPulse.
 * <p>
 * Maneja las solicitudes públicas bajo el prefijo {@code /api/auth}, gestionando el registro
 * de ciudadanos (HTTP 201 Created) y el inicio de sesión con emisión de tokens JWT (HTTP 200 OK).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Constructor con inyección del servicio de autenticación.
     *
     * @param authService Servicio que contiene la lógica de negocio de autenticación y registro.
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint para el registro de nuevos ciudadanos en la plataforma.
     * <p>
     * Recibe los datos validados del usuario y, si el correo está disponible y las credenciales
     * son válidas, crea la cuenta y responde con código HTTP 201 Created.
     * </p>
     *
     * @param request Datos de registro con validaciones de Bean Validation.
     * @return {@link ResponseEntity} con código 201 Created y cuerpo {@link RegisterResponse}.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint para el inicio de sesión y autenticación de usuarios.
     * <p>
     * Valida las credenciales proporcionadas (email y contraseña) y, ante éxito,
     * retorna código HTTP 200 OK acompañado del token JWT y metadatos de sesión.
     * </p>
     *
     * @param request Datos de inicio de sesión con correo y contraseña.
     * @return {@link ResponseEntity} con código 200 OK y cuerpo {@link AuthResponse}.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
