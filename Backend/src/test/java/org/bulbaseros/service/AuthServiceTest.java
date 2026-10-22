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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para la capa de servicios de autenticación {@link AuthService}.
 * <p>
 * Valida el registro de usuarios, la verificación de unicidad de correo electrónico,
 * la encriptación de contraseñas con BCrypt, el inicio de sesión y el manejo de credenciales inválidas.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    /**
     * Verifica que un usuario se registre correctamente cuando el correo electrónico está disponible.
     */
    @Test
    @DisplayName("Debería registrar un nuevo ciudadano exitosamente y retornar confirmación")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Carlos Ruiz");
        request.setEmail("carlos@urbanpulse.es");
        request.setPassword("Password123");

        when(userRepository.existsByEmail("carlos@urbanpulse.es")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPasswordHash");

        Usuario savedUser = new Usuario("carlos@urbanpulse.es", "encodedPasswordHash", "Carlos Ruiz", RolUsuario.CITIZEN);
        savedUser.setId(1L);
        when(userRepository.save(any(Usuario.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("Usuario registrado exitosamente", response.getMessage());
        assertEquals("carlos@urbanpulse.es", response.getEmail());

        ArgumentCaptor<Usuario> userCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(userRepository).save(userCaptor.capture());
        Usuario capturedUser = userCaptor.getValue();
        assertEquals("carlos@urbanpulse.es", capturedUser.getEmail());
        assertEquals("encodedPasswordHash", capturedUser.getPasswordHash());
        assertEquals("Carlos Ruiz", capturedUser.getFullName());
        assertEquals(RolUsuario.CITIZEN, capturedUser.getRole());
    }

    /**
     * Verifica que el correo electrónico sea normalizado (espacios eliminados y en minúsculas) al registrar.
     */
    @Test
    @DisplayName("Debería normalizar el correo electrónico a minúsculas y sin espacios al registrar")
    void shouldNormalizeEmailOnRegister() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("María Gómez");
        request.setEmail("   MARIA@UrbanPulse.ES   ");
        request.setPassword("Password123");

        when(userRepository.existsByEmail("maria@urbanpulse.es")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hash123");

        Usuario savedUser = new Usuario("maria@urbanpulse.es", "hash123", "María Gómez", RolUsuario.CITIZEN);
        when(userRepository.save(any(Usuario.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertEquals("maria@urbanpulse.es", response.getEmail());
        verify(userRepository).existsByEmail("maria@urbanpulse.es");
    }

    /**
     * Verifica que se lance {@link EmailAlreadyExistsException} si el correo ya existe en el sistema.
     */
    @Test
    @DisplayName("Debería lanzar EmailAlreadyExistsException cuando el correo ya está registrado")
    void shouldThrowEmailAlreadyExistsExceptionWhenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Carlos Ruiz");
        request.setEmail("carlos@urbanpulse.es");
        request.setPassword("Password123");

        when(userRepository.existsByEmail("carlos@urbanpulse.es")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(Usuario.class));
    }

    /**
     * Verifica que el inicio de sesión sea exitoso cuando las credenciales son correctas.
     */
    @Test
    @DisplayName("Debería autenticar al usuario y retornar AuthResponse con token JWT")
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("carlos@urbanpulse.es", "Password123");
        Usuario usuario = new Usuario("carlos@urbanpulse.es", "encodedPasswordHash", "Carlos Ruiz", RolUsuario.CITIZEN);

        when(userRepository.findByEmail("carlos@urbanpulse.es")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123", "encodedPasswordHash")).thenReturn(true);
        when(jwtService.generateToken(usuario)).thenReturn("mocked.jwt.token");
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400L, response.getExpiresIn());
        assertEquals("CITIZEN", response.getRole());
        assertEquals("Carlos Ruiz", response.getFullName());
    }

    /**
     * Verifica que el correo se normalice a minúsculas y sin espacios al iniciar sesión.
     */
    @Test
    @DisplayName("Debería normalizar el correo electrónico al iniciar sesión")
    void shouldNormalizeEmailOnLogin() {
        LoginRequest request = new LoginRequest("   CARLOS@UrbanPulse.ES   ", "Password123");
        Usuario usuario = new Usuario("carlos@urbanpulse.es", "encodedPasswordHash", "Carlos Ruiz", RolUsuario.CITIZEN);

        when(userRepository.findByEmail("carlos@urbanpulse.es")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123", "encodedPasswordHash")).thenReturn(true);
        when(jwtService.generateToken(usuario)).thenReturn("mocked.jwt.token");
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        verify(userRepository).findByEmail("carlos@urbanpulse.es");
    }

    /**
     * Verifica que se lance {@link InvalidCredentialsException} cuando el usuario no existe.
     */
    @Test
    @DisplayName("Debería lanzar InvalidCredentialsException si el usuario no existe en la base de datos")
    void shouldThrowInvalidCredentialsWhenUserNotFound() {
        LoginRequest request = new LoginRequest("inexistente@urbanpulse.es", "Password123");

        when(userRepository.findByEmail("inexistente@urbanpulse.es")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(Usuario.class));
    }

    /**
     * Verifica que se lance {@link InvalidCredentialsException} si la contraseña no coincide.
     */
    @Test
    @DisplayName("Debería lanzar InvalidCredentialsException si la contraseña suministrada no coincide")
    void shouldThrowInvalidCredentialsWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest("carlos@urbanpulse.es", "PasswordErronea");
        Usuario usuario = new Usuario("carlos@urbanpulse.es", "encodedPasswordHash", "Carlos Ruiz", RolUsuario.CITIZEN);

        when(userRepository.findByEmail("carlos@urbanpulse.es")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("PasswordErronea", "encodedPasswordHash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any(Usuario.class));
    }

    /**
     * Verifica que se lance {@link IllegalArgumentException} si las solicitudes suministradas son nulas.
     */
    @Test
    @DisplayName("Debería lanzar IllegalArgumentException si la solicitud de registro o login es nula")
    void shouldThrowIllegalArgumentExceptionWhenRequestIsNull() {
        assertThrows(IllegalArgumentException.class, () -> authService.register(null));
        assertThrows(IllegalArgumentException.class, () -> authService.login(null));
    }
}
