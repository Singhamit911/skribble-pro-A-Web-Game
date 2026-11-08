package com.skribblepro.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionValidationFilterTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveHashOperations<String, Object, Object> hashOperations;

    @Mock
    private GatewayFilterChain filterChain;

    private SessionValidationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SessionValidationFilter(redisTemplate);
    }

    @Test
    @DisplayName("Should bypass public endpoints without token: POST /sessions, /actuator, /ws, OPTIONS")
    void shouldBypassPublicEndpoints() {
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // POST /sessions
        MockServerWebExchange exchangePostSessions = MockServerWebExchange.from(
                MockServerHttpRequest.post("/sessions").build()
        );
        StepVerifier.create(filter.filter(exchangePostSessions, filterChain)).verifyComplete();

        // GET /actuator/health
        MockServerWebExchange exchangeActuator = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build()
        );
        StepVerifier.create(filter.filter(exchangeActuator, filterChain)).verifyComplete();

        // WS handshake
        MockServerWebExchange exchangeWs = MockServerWebExchange.from(
                MockServerHttpRequest.get("/ws/game").build()
        );
        StepVerifier.create(filter.filter(exchangeWs, filterChain)).verifyComplete();

        // CORS OPTIONS preflight
        MockServerWebExchange exchangeOptions = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.OPTIONS, "/rooms").build()
        );
        StepVerifier.create(filter.filter(exchangeOptions, filterChain)).verifyComplete();

        verify(redisTemplate, never()).opsForHash();
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when Authorization header is missing")
    void shouldReturn401WhenAuthHeaderMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/rooms").build()
        );

        StepVerifier.create(filter.filter(exchange, filterChain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when Bearer token is empty or invalid format")
    void shouldReturn401WhenTokenFormatInvalid() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/rooms").header("Authorization", "Bearer ").build()
        );

        StepVerifier.create(filter.filter(exchange, filterChain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when session is not found or expired in Redis")
    void shouldReturn401WhenSessionNotFoundInRedis() {
        String token = "expired-token-123";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("session:" + token)).thenReturn(Flux.empty());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/rooms").header("Authorization", "Bearer " + token).build()
        );

        StepVerifier.create(filter.filter(exchange, filterChain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("Should slide TTL and forward X-Guest-* headers when session is valid")
    void shouldSlideTtlAndForwardHeadersWhenSessionValid() {
        String token = "valid-token-xyz";
        String key = "session:" + token;

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        Map.Entry<Object, Object> entry1 = Map.entry("guestId", "user-uuid-123");
        Map.Entry<Object, Object> entry2 = Map.entry("name", "Sangi");
        Map.Entry<Object, Object> entry3 = Map.entry("color", "#FF6B6B");

        when(hashOperations.entries(key)).thenReturn(Flux.just(entry1, entry2, entry3));
        when(redisTemplate.expire(key, Duration.ofHours(1))).thenReturn(Mono.just(true));

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(filterChain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/rooms/XK92TZ").header("Authorization", "Bearer " + token).build()
        );

        StepVerifier.create(filter.filter(exchange, filterChain)).verifyComplete();

        verify(redisTemplate).expire(key, Duration.ofHours(1));

        ServerWebExchange captured = exchangeCaptor.getValue();
        assertThat(captured.getRequest().getHeaders().getFirst("X-Guest-Id")).isEqualTo("user-uuid-123");
        assertThat(captured.getRequest().getHeaders().getFirst("X-Guest-Name")).isEqualTo("Sangi");
        assertThat(captured.getRequest().getHeaders().getFirst("X-Guest-Color")).isEqualTo("#FF6B6B");
    }
}

