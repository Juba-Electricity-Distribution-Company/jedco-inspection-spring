package com.jedco.jedcoinspectionspring.configuration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jedco.jedcoinspectionspring.services.JwtService;
import com.jedco.jedcoinspectionspring.services.JwtServiceImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTests {
    // Test-only signing material, independent of deployment configuration.
    private static final byte[] KEY = "test-signing-key-for-jwt-filter-32-bytes".getBytes(StandardCharsets.UTF_8);
    private final UserDetailsService users = mock(UserDetailsService.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final Logger logger = (Logger) LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private Level previousLevel;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        JwtServiceImpl jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "jwtSigningKey", Encoders.BASE64.encode(KEY));
        filter = new JwtAuthenticationFilter(jwtService, users);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        logger.detachAppender(logs);
        logger.setLevel(previousLevel);
        logs.stop();
    }

    @Test
    void expiredTokenReturnsUnauthorizedWithoutContinuingOrLoggingToken() throws Exception {
        String token = token(KEY, "inspector", -60_000);
        reject(token, "Authentication token has expired.");
        assertThat(logs.list).hasSize(1);
        assertThat(logs.list.getFirst().getFormattedMessage()).isEqualTo("Rejected expired JWT");
        assertThat(logs.list.getFirst().getLevel()).isEqualTo(Level.DEBUG);
    }

    @Test
    void malformedTokenReturnsUnauthorized() throws Exception {
        reject("not-a-jwt", "Invalid authentication token.");
    }

    @Test
    void invalidSignatureReturnsUnauthorized() throws Exception {
        byte[] otherKey = "different-test-signing-key-32-bytes".getBytes(StandardCharsets.UTF_8);
        reject(token(otherKey, "inspector", 60_000), "Invalid authentication token.");
    }

    @Test
    void emptyBearerTokenReturnsUnauthorized() throws Exception {
        reject("", "Invalid authentication token.");
    }

    @Test
    void tokenWithoutSubjectReturnsUnauthorized() throws Exception {
        reject(token(KEY, null, 60_000), "Invalid authentication token.");
    }

    @Test
    void expirationDuringValidationAlsoStopsTheChain() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.extractUserName("token")).thenReturn("inspector");
        when(users.loadUserByUsername("inspector")).thenReturn(User.withUsername("inspector")
                .password("unused").authorities("inspect").build());
        when(jwtService.isTokenValid(eq("token"), any())).thenThrow(
                new io.jsonwebtoken.ExpiredJwtException(null, null, "expired"));
        filter = new JwtAuthenticationFilter(jwtService, users);
        request.addHeader("Authorization", "Bearer token");
        filter.doFilter(request, response, chain);
        assertUnauthorized("Authentication token has expired.");
    }

    @Test
    void validTokenAuthenticatesAndContinues() throws Exception {
        when(users.loadUserByUsername("inspector")).thenReturn(User.withUsername("inspector")
                .password("unused").authorities("inspect").build());
        request.addHeader("Authorization", "Bearer " + token(KEY, "inspector", 60_000));
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("inspector");
    }

    @Test
    void missingBearerHeaderContinuesWithoutAuthentication() throws Exception {
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(users);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void reject(String token, String message) throws Exception {
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, response, chain);
        assertUnauthorized(message);
        verifyNoInteractions(users);
        assertThat(logs.list).allSatisfy(event -> {
            assertThat(event.getThrowableProxy()).isNull();
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            if (!token.isEmpty()) {
                assertThat(event.getFormattedMessage()).doesNotContain(token);
            }
        });
    }

    private void assertUnauthorized(String message) throws Exception {
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
        assertThat(body.size()).isEqualTo(3);
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("title").asText()).isEqualTo("Unauthorized");
        assertThat(body.get("message").asText()).isEqualTo(message);
        verifyNoInteractions(chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private String token(byte[] key, String subject, long expiresInMillis) {
        return Jwts.builder().setSubject(subject)
                .setExpiration(new Date(System.currentTimeMillis() + expiresInMillis))
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256).compact();
    }
}
