package org.bulbaseros.service;

import org.bulbaseros.dto.AuthResponse;
import org.bulbaseros.dto.LoginRequest;
import org.bulbaseros.dto.RegisterRequest;
import org.bulbaseros.dto.RegisterResponse;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.exception.EmailAlreadyExistsException;
import org.bulbaseros.exception.InvalidCredentialsException;
import org.bulbaseros.repository.UserRepository;
import org.bulbaseros.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de lógica de negocio para la gestión de autenticación y registro de usuarios en UrbanPulse.
 * <p>
 * Implementa la validación de unicidad de correo, el cifrado seguro de contraseñas mediante BCrypt,
 * la verificación de credenciales y la emisión de tokens JWT bajo una arquitectura sin estado.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Constructor con inyección de dependencias para el servicio de autenticación.
     *
     * @param userRepository  Repositorio JPA para la persistencia y consulta de usuarios.
     * @param passwordEncoder Componente para el hashing y verificación segura de contraseñas.
     * @param jwtService      Servicio para la generación y firma de tokens JWT.
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registra a un nuevo usuario en la plataforma con el rol por defecto {@link RolUsuario#CITIZEN}.
     * <p>
     * Normaliza el correo electrónico eliminando espacios y convirtiéndolo a minúsculas,
     * comprueba la unicidad en la base de datos, encripta la contraseña y persiste la entidad.
     * </p>
     *
     * @param request Datos de la solicitud de registro validados previamente.
     * @return DTO {@link RegisterResponse} con mensaje de éxito y correo registrado.
     * @throws IllegalArgumentException    si la solicitud suministrada es nula.
     * @throws EmailAlreadyExistsException si el correo electrónico ya se encuentra registrado.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de registro no puede ser nula");
        }

        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("El correo electrónico ya está registrado.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        String fullName = request.getFullName() != null ? request.getFullName().trim() : "";

        Usuario usuario = new Usuario(normalizedEmail, encodedPassword, fullName, RolUsuario.CITIZEN);
        Usuario savedUser = userRepository.save(usuario);

        return new RegisterResponse("Usuario registrado exitosamente", savedUser.getEmail());
    }

    /**
     * Autentica a un usuario mediante sus credenciales (correo electrónico y contraseña).
     * <p>
     * Normaliza el correo, busca al usuario registrado y comprueba el hash de la contraseña.
     * En caso de éxito emite un token JWT con una duración estándar de 24 horas (86.400 segundos).
     * Ante cualquier discrepancia, lanza una excepción de credenciales inválidas para evitar
     * la enumeración de usuarios.
     * </p>
     *
     * @param request Datos de la solicitud de inicio de sesión.
     * @return DTO {@link AuthResponse} con token JWT, tipo Bearer, expiración, rol y nombre completo.
     * @throws IllegalArgumentException    si la solicitud suministrada es nula.
     * @throws InvalidCredentialsException si el usuario no existe o la contraseña no coincide.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de inicio de sesión no puede ser nula");
        }

        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        Usuario usuario = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas."));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenciales inválidas.");
        }

        String token = jwtService.generateToken(usuario);
        long expiresInSeconds = jwtService.getJwtExpiration() > 0 ? (jwtService.getJwtExpiration() / 1000L) : 86400L;

        return new AuthResponse(token, "Bearer", expiresInSeconds, usuario.getRole().name(), usuario.getFullName());
    }
}
