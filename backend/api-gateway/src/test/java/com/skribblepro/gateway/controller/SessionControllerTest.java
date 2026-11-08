package com.skribblepro.gateway.controller;

import com.skribblepro.dto.session.CreateSessionRequest;
import com.skribblepro.dto.session.SessionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveHashOperations<String, Object, Object> hashOperations;

    private SessionController sessionController;

    @BeforeEach
    void setUp() {
        sessionController = new SessionController(redisTemplate);
    }

    @Test
    @DisplayName("createSession: Valid request creates guest session and stores in Redis with 1h TTL")
    void createSession_ValidInput_Success() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.putAll(anyString(), anyMap())).thenReturn(Mono.just(true));
        when(redisTemplate.expire(anyString(), eq(Duration.ofHours(1)))).thenReturn(Mono.just(true));

        CreateSessionRequest request = new CreateSessionRequest("Sangi", "#FF6B6B");

        Mono<SessionResponse> result = sessionController.createSession(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.sessionToken()).isNotBlank();
                    assertThat(response.guestId()).isNotBlank();
                    assertThat(response.name()).isEqualTo("Sangi");
                    assertThat(response.color()).isEqualTo("#FF6B6B");
                })
                .verifyComplete();

        verify(redisTemplate).expire(anyString(), eq(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("createSession: Name shorter than 3 characters throws 400 Bad Request")
    void createSession_NameTooShort_ThrowsBadRequest() {
        CreateSessionRequest request = new CreateSessionRequest("Al", "#FF6B6B");

        StepVerifier.create(sessionController.createSession(request))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST
                        && rse.getReason().contains("between 3 and 20 characters"))
                .verify();
    }

    @Test
    @DisplayName("createSession: Name longer than 20 characters throws 400 Bad Request")
    void createSession_NameTooLong_ThrowsBadRequest() {
        CreateSessionRequest request = new CreateSessionRequest("ThisNameIsWayTooLongForAUsername123", "#FF6B6B");

        StepVerifier.create(sessionController.createSession(request))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("createSession: Invalid hex color throws 400 Bad Request")
    void createSession_InvalidColor_ThrowsBadRequest() {
        CreateSessionRequest request = new CreateSessionRequest("ValidPlayer", "red");

        StepVerifier.create(sessionController.createSession(request))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST
                        && rse.getReason().contains("valid hex code"))
                .verify();
    }

    @Test
    @DisplayName("deleteSession: Valid Bearer token deletes session key from Redis")
    void deleteSession_ValidToken_Success() {
        String token = "valid-session-token-123";
        when(redisTemplate.delete("session:" + token)).thenReturn(Mono.just(1L));

        Mono<Void> result = sessionController.deleteSession("Bearer " + token);

        StepVerifier.create(result)
                .verifyComplete();

        verify(redisTemplate).delete("session:" + token);
    }

    @Test
    @DisplayName("deleteSession: Missing or non-Bearer authorization header throws 401 Unauthorized")
    void deleteSession_MissingBearerToken_ThrowsUnauthorized() {
        StepVerifier.create(sessionController.deleteSession(null))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.UNAUTHORIZED)
                .verify();

        StepVerifier.create(sessionController.deleteSession("Basic 12345"))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.UNAUTHORIZED)
                .verify();
    }
}

