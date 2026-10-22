package org.bulbaseros.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.bulbaseros.dto.ErrorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link JwtAuthenticationFilter}.
 * <p>
 * Verifica la extracción robusta de cabeceras {@code Authorization}, tolerancia a variantes
 * de mayúsculas/minúsculas y espacios múltiples, inyección en {@link SecurityContextHolder}
 * para roles {@code CITIZEN}, {@code OPERATOR} y {@code ADMIN}, delegación controlada en
 * {@link JwtAuthenticationEntryPoint} ante tokens expirados o corruptos, y paso transparente
 * en rutas públicas ({@code /api/auth/**}, {@code /h2-console/**}).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET_KEY =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B59703273357638792F423F4528482B4D6251655468576D5A7134743777217A25432A";
    private static final long EXPIRATION_MS = 3600000L; // 1 hora

    private JwtService jwtService;
    private JwtAuthenticationEntryPoint entryPoint;
    private JwtAuthenticationFilter filter;
    private ObjectMapper objectMapper;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = new JwtService(SECRET_KEY, EXPIRATION_MS);
        entryPoint = new JwtAuthenticationEntryPoint();
        filter = new JwtAuthenticationFilter(jwtService, entryPoint);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = spy(new MockFilterChain());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Debería autenticar un token válido con rol CITIZEN e inyectar ROLE_CITIZEN en el contexto")
    void shouldAuthenticateValidTokenWithCitizenRole() throws Exception {
        String token = jwtService.generateToken("ciudadano@urbanpulse.es", "CITIZEN");
        request.setRequestURI("/api/incidents");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "El contexto de seguridad debe contener una autenticación");
        assertEquals("ciudadano@urbanpulse.es", auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CITIZEN")));
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería autenticar un token válido con rol OPERATOR e inyectar ROLE_OPERATOR")
    void shouldAuthenticateValidTokenWithOperatorRole() throws Exception {
        String token = jwtService.generateToken("operador@urbanpulse.es", "OPERATOR");
        request.setRequestURI("/api/incidents/1/status");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("operador@urbanpulse.es", auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OPERATOR")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería autenticar un token válido con rol ADMIN e inyectar ROLE_ADMIN")
    void shouldAuthenticateValidTokenWithAdminRole() throws Exception {
        String token = jwtService.generateToken("admin@urbanpulse.es", "ADMIN");
        request.setRequestURI("/api/incidents/1/status");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("admin@urbanpulse.es", auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería tolerar prefijo 'bearer' en minúsculas")
    void shouldSupportLowercaseBearerPrefix() throws Exception {
        String token = jwtService.generateToken("ciudadano@urbanpulse.es", "CITIZEN");
        request.setRequestURI("/api/incidents");
        request.addHeader(HttpHeaders.AUTHORIZATION, "bearer " + token);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("ciudadano@urbanpulse.es", auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CITIZEN")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería tolerar prefijo 'BEARER' en mayúsculas completas")
    void shouldSupportUppercaseBearerPrefix() throws Exception {
        String token = jwtService.generateToken("ciudadano@urbanpulse.es", "CITIZEN");
        request.setRequestURI("/api/incidents");
        request.addHeader(HttpHeaders.AUTHORIZATION, "BEARER " + token);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("ciudadano@urbanpulse.es", auth.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería tolerar prefijo con mezcla de mayúsculas y minúsculas 'BeArEr'")
    void shouldSupportMixedCaseBearerPrefix() throws Exception {
        String token = jwtService.generateToken("ciudadano@urbanpulse.es", "CITIZEN");
        request.setRequestURI("/api/incidents");
        request.addHeader(HttpHeaders.AUTHORIZATION, "BeArEr " + token);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("ciudadano@urbanpulse.es", auth.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería tolerar múltiples espacios entre el prefijo Bearer y el token")
    void shouldSupportMultipleSpacesBetweenBearerAndToken() throws Exception {
        String token = jwtService.generateToken("ciudadano@urbanpulse.es", "CITIZEN");
        request.setRequestURI("/api/incidents");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer       " + token);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("ciudadano@urbanpulse.es", auth.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería tolerar espacios en blanco al inicio y al final de la cabecera")
    void shouldSupportLeadingAndTrailingSpacesInHeader() throws Exception {
        String token = jwtService.generateToken("ciudadano@urbanpulse.es", "CITIZEN");
        request.setRequestURI("/api/incidents");
        request.addHeader(HttpHeaders.AUTHORIZATION, "   Bearer " + token + "   ");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("ciudadano@urbanpulse.es", auth.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería continuar la cadena sin alterar el contexto si no hay cabecera Authorization")
    void shouldHandleMissingAuthorizationHeaderGracefully() throws Exception {
        request.setRequestURI("/api/incidents");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "El contexto debe permanecer nulo");
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería continuar la cadena sin alterar el contexto si la cabecera está vacía")
    void shouldHandleBlankAuthorizationHeader() throws Exception {
        request.setRequestURI("/api/incidents");
        request.addHeader(HttpHeaders.AUTHORIZATION, "    ");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería continuar la cadena sin autenticar si el prefijo no es Bearer")
    void shouldHandleInvalidHeaderPrefixWithoutBearer() throws Exception {
        request.setRequestURI("/api/incidents");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería continuar la cadena sin autenticar si la cabecera es solo 'Bearer' sin token")
    void shouldHandleBearerHeaderWithoutToken() throws Exception {
        request.setRequestURI("/api/incidents");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);

        MockHttpServletRequest requestWithSpaces = new MockHttpServletRequest();
        requestWithSpaces.setRequestURI("/api/incidents");
        requestWithSpaces.addHeader(HttpHeaders.AUTHORIZATION, "Bearer    ");
        MockHttpServletResponse responseWithSpaces = new MockHttpServletResponse();
        MockFilterChain filterChainWithSpaces = spy(new MockFilterChain());

        filter.doFilter(requestWithSpaces, responseWithSpaces, filterChainWithSpaces);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChainWithSpaces).doFilter(requestWithSpaces, responseWithSpaces);
    }

    @Test
    @DisplayName("Debería emitir HTTP 401 con JSON estructurado si el token ha expirado")
    void shouldHandleExpiredTokenAndDelegateToEntryPoint() throws Exception {
        // JwtService con expiración negativa (-1 segundo) para forzar expiración inmediata
        JwtService expiredJwtService = new JwtService(SECRET_KEY, -1000L);
        String expiredToken = expiredJwtService.generateToken("expirado@urbanpulse.es", "CITIZEN");

        request.setRequestURI("/api/incidents");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        String content = response.getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(content, ErrorResponse.class);

        assertEquals(401, errorResponse.getStatus());
        assertEquals("Unauthorized", errorResponse.getError());
        assertEquals("Token de autenticación expirado o inválido", errorResponse.getMessage());
        assertEquals("/api/incidents", errorResponse.getPath());
    }

    @Test
    @DisplayName("Debería emitir HTTP 401 con JSON estructurado si el token está malformado")
    void shouldHandleMalformedTokenAndDelegateToEntryPoint() throws Exception {
        request.setRequestURI("/api/incidents/1/status");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token.malformado.invalido");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        String content = response.getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(content, ErrorResponse.class);

        assertEquals(401, errorResponse.getStatus());
        assertEquals("Unauthorized", errorResponse.getError());
        assertEquals("Token de autenticación expirado o inválido", errorResponse.getMessage());
        assertEquals("/api/incidents/1/status", errorResponse.getPath());
    }

    @Test
    @DisplayName("Debería manejar JwtException o IllegalArgumentException delegando en el entry point de forma segura sin lanzar 500")
    void shouldHandleJwtExceptionSafelyWithoutThrowing500() throws Exception {
        JwtService mockJwtService = Mockito.mock(JwtService.class);
        when(mockJwtService.extractEmail(anyString())).thenThrow(new IllegalArgumentException("Claim corrupto"));

        JwtAuthenticationFilter mockFilter = new JwtAuthenticationFilter(mockJwtService, entryPoint);

        request.setRequestURI("/api/protected");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token_con_error");

        mockFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());

        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(401, errorResponse.getStatus());
        assertEquals("Token de autenticación expirado o inválido", errorResponse.getMessage());
    }

    @Test
    @DisplayName("Debería omitir la validación y permitir paso transparente en endpoint público /api/auth/login incluso con token inválido")
    void shouldSkipFilterOnPublicAuthLoginEndpointEvenWithInvalidToken() throws Exception {
        request.setRequestURI("/api/auth/login");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token.completamente.invalido");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    @DisplayName("Debería omitir la validación y permitir paso transparente en endpoint público /api/auth/register con token expirado")
    void shouldSkipFilterOnPublicAuthRegisterEndpointWithExpiredToken() throws Exception {
        JwtService expiredJwtService = new JwtService(SECRET_KEY, -1000L);
        String expiredToken = expiredJwtService.generateToken("test@urbanpulse.es", "CITIZEN");

        request.setRequestURI("/api/auth/register");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    @DisplayName("Debería omitir la validación y permitir paso transparente en rutas de la consola H2")
    void shouldSkipFilterOnH2ConsoleEndpoint() throws Exception {
        request.setRequestURI("/h2-console/index.html");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token.invalido");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    @DisplayName("Debería autenticar al usuario sin autoridades si el token no incluye el claim role")
    void shouldHandleTokenWithNullOrEmptyRoleGracefully() throws Exception {
        String tokenWithoutRole = jwtService.generateToken("sinrol@urbanpulse.es", null);
        request.setRequestURI("/api/user/profile");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutRole);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("sinrol@urbanpulse.es", auth.getPrincipal());
        assertTrue(auth.getAuthorities().isEmpty(), "No debe poseer autoridades si el rol es nulo");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("No debería sobreescribir una autenticación ya presente en el SecurityContextHolder")
    void shouldNotOverwriteExistingAuthentication() throws Exception {
        Authentication existingAuth = new UsernamePasswordAuthenticationToken(
                "previo@urbanpulse.es", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        String newToken = jwtService.generateToken("nuevo@urbanpulse.es", "CITIZEN");
        request.setRequestURI("/api/incidents");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newToken);

        filter.doFilter(request, response, filterChain);

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(currentAuth);
        assertEquals("previo@urbanpulse.es", currentAuth.getPrincipal(), "La autenticación previa no debe modificarse");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("shouldNotFilter debe retornar true para rutas públicas y false para rutas protegidas")
    void shouldEvaluateShouldNotFilterAccurately() throws Exception {
        MockHttpServletRequest reqAuth = new MockHttpServletRequest("POST", "/api/auth/login");
        assertTrue(filter.shouldNotFilter(reqAuth));

        MockHttpServletRequest reqRegister = new MockHttpServletRequest("POST", "/api/auth/register");
        assertTrue(filter.shouldNotFilter(reqRegister));

        MockHttpServletRequest reqH2 = new MockHttpServletRequest("GET", "/h2-console/index.html");
        assertTrue(filter.shouldNotFilter(reqH2));

        MockHttpServletRequest reqIncidents = new MockHttpServletRequest("GET", "/api/incidents");
        assertFalse(filter.shouldNotFilter(reqIncidents));

        MockHttpServletRequest reqPatch = new MockHttpServletRequest("PATCH", "/api/incidents/1/status");
        assertFalse(filter.shouldNotFilter(reqPatch));

        assertFalse(filter.shouldNotFilter(null));
    }

    @Test
    @DisplayName("Debería soportar constructor con un solo parámetro instanciando JwtAuthenticationEntryPoint por defecto")
    void shouldSupportSingleParameterConstructor() {
        JwtAuthenticationFilter singleParamFilter = new JwtAuthenticationFilter(jwtService);
        assertNotNull(singleParamFilter);
    }
}
