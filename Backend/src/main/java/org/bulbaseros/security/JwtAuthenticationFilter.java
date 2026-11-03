package org.bulbaseros.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtro de seguridad perimetral que intercepta cada petición HTTP una sola vez ({@link OncePerRequestFilter})
 * para extraer, validar y autenticar tokens JWT contenidos en la cabecera {@code Authorization}.
 * <p>
 * Este filtro opera de forma previa a los filtros convencionales de Spring Security, deserializando
 * los claims del token (correo electrónico como {@code sub} y rol como {@code role}) y poblando
 * el {@link SecurityContextHolder} con la correspondiente autoridad {@code ROLE_<ROL>}.
 * </p>
 * <p>
 * Asimismo, tolera variantes de mayúsculas/minúsculas y espacios múltiples en el esquema {@code Bearer},
 * omite de forma transparente la validación en rutas públicas designadas (ej. {@code /api/auth/**}, {@code /h2-console/**})
 * y delega de manera controlada cualquier fallo de autenticación o token corrupto/expirado en
 * {@link JwtAuthenticationEntryPoint} impidiendo errores 500 no capturados.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * Patrón de expresión regular para identificar y extraer el token tras el prefijo {@code Bearer},
     * insensible a mayúsculas/minúsculas y tolerando múltiples espacios en blanco separadores.
     */
    private static final Pattern BEARER_PATTERN = Pattern.compile("^(?i)bearer\\s+(.+)$");

    private final JwtService jwtService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Constructor por defecto para contextos donde las dependencias se configuran posteriormente.
     */
    public JwtAuthenticationFilter() {
        this(null, new JwtAuthenticationEntryPoint());
    }

    /**
     * Constructor de conveniencia que inicializa el filtro con una instancia por defecto de {@link JwtAuthenticationEntryPoint}.
     *
     * @param jwtService servicio de generación, extracción y verificación de tokens JWT.
     */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this(jwtService, new JwtAuthenticationEntryPoint());
    }

    /**
     * Constructor principal para inyección de dependencias de Spring.
     *
     * @param jwtService               servicio de generación, extracción y verificación de tokens JWT.
     * @param authenticationEntryPoint manejador de respuestas estructuradas HTTP 401 Unauthorized.
     */
    @Autowired
    public JwtAuthenticationFilter(@Autowired(required = false) JwtService jwtService,
                                   @Autowired(required = false) JwtAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.authenticationEntryPoint = (authenticationEntryPoint != null)
                ? authenticationEntryPoint
                : new JwtAuthenticationEntryPoint();
    }

    /**
     * Determina si la petición entrante debe excluirse del filtrado de autenticación JWT.
     * <p>
     * Se omiten de forma transparente los endpoints de la API de autenticación pública ({@code /api/auth/**})
     * y las rutas de la consola H2 ({@code /h2-console/**}).
     * </p>
     *
     * @param request la petición HTTP entrante a evaluar.
     * @return {@code true} si la ruta es pública y no requiere validación JWT; {@code false} en caso contrario.
     * @throws ServletException si ocurre un fallo al inspeccionar la petición.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        if (request == null) {
            return false;
        }
        return isPublicPath(request);
    }

    /**
     * Ejecuta la lógica principal de filtrado de seguridad por cada petición HTTP.
     * <p>
     * Si la petición apunta a una ruta pública, permite el paso directo sin validación.
     * Si contiene una cabecera {@code Authorization} con prefijo Bearer, valida el token,
     * extrae la identidad del usuario y su rol funcional, registrando la autenticación en
     * {@link SecurityContextHolder}. Ante tokens corruptos o expirados, delega de inmediato
     * en {@link JwtAuthenticationEntryPoint} emitiendo una respuesta 401 estructurada.
     * </p>
     *
     * @param request     la petición HTTP entrante.
     * @param response    la respuesta HTTP saliente.
     * @param filterChain la cadena de filtros de servlet de Spring Security.
     * @throws ServletException en caso de error interno en el procesamiento del servlet.
     * @throws IOException      en caso de error de entrada/salida durante la escritura de la respuesta.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Paso transparente en rutas públicas o si el servicio JWT no está provisto en el contexto
        if (jwtService == null || "OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = extractTokenFromHeader(request);

        // Si no se proporcionó cabecera Bearer, continuar la cadena sin alterar el contexto de seguridad
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String email = jwtService.extractEmail(token);

            if (email == null || email.isBlank()) {
                handleAuthenticationFailure(request, response, "Token de autenticación expirado o inválido", null);
                return;
            }

            if (jwtService.isTokenExpired(token)) {
                handleAuthenticationFailure(request, response, "Token de autenticación expirado o inválido", null);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String role = jwtService.extractRole(token);
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                if (role != null && !role.isBlank()) {
                    String roleName = role.trim().toUpperCase();
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    authorities.add(new SimpleGrantedAuthority(roleName));
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.debug("Token JWT expirado al acceder a {}: {}", request.getRequestURI(), e.getMessage());
            handleAuthenticationFailure(request, response, "Token de autenticación expirado o inválido", e);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Error de validación JWT al acceder a {}: {}", request.getRequestURI(), e.getMessage());
            handleAuthenticationFailure(request, response, "Token de autenticación expirado o inválido", e);
        }
    }

    /**
     * Extrae el token JWT limpio de la cabecera {@code Authorization}, verificando la presencia
     * del prefijo {@code Bearer} con tolerancia a mayúsculas/minúsculas y espacios múltiples.
     *
     * @param request la petición HTTP de la cual extraer la cabecera.
     * @return el token JWT extraído, o {@code null} si la cabecera está ausente, vacía o no cumple el formato Bearer.
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        Matcher matcher = BEARER_PATTERN.matcher(authHeader.trim());
        if (matcher.matches()) {
            String token = matcher.group(1).trim();
            return token.isEmpty() ? null : token;
        }

        return null;
    }

    /**
     * Evalúa si la petición corresponde a una ruta pública exenta de autenticación obligatoria.
     *
     * @param request la petición HTTP a inspeccionar.
     * @return {@code true} si la ruta es pública; {@code false} en caso contrario.
     */
    private boolean isPublicPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String servletPath = request.getServletPath();
        return matchesPublicPattern(uri) || matchesPublicPattern(servletPath);
    }

    /**
     * Comprueba si una ruta textual coincide con los prefijos públicos permitidos.
     *
     * @param path cadena con la ruta a comprobar.
     * @return {@code true} si coincide con {@code /api/auth/**} o {@code /h2-console/**}; {@code false} si no.
     */
    private boolean matchesPublicPattern(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/api/auth/") || path.equals("/api/auth")
                || path.startsWith("/h2-console/") || path.equals("/h2-console");
    }

    /**
     * Gestiona de manera centralizada los fallos de autenticación capturados en el filtro,
     * limpiando el contexto de seguridad, fijando el atributo de error en la petición y delegando
     * en {@link JwtAuthenticationEntryPoint} para emitir la respuesta 401 en formato JSON.
     *
     * @param request      la petición HTTP entrante.
     * @param response     la respuesta HTTP saliente.
     * @param errorMessage mensaje descriptivo del fallo de autenticación.
     * @param cause        excepción de origen que motivó el fallo, o {@code null}.
     * @throws IOException      en caso de error al serializar la respuesta de error.
     * @throws ServletException en caso de error interno del servlet container.
     */
    private void handleAuthenticationFailure(HttpServletRequest request,
                                             HttpServletResponse response,
                                             String errorMessage,
                                             Exception cause) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        request.setAttribute("auth_error_message", errorMessage);

        if (authenticationEntryPoint != null) {
            AuthenticationException authException = (cause instanceof AuthenticationException ae)
                    ? ae
                    : new BadCredentialsException(errorMessage, cause);
            authenticationEntryPoint.commence(request, response, authException);
        }
    }
}
