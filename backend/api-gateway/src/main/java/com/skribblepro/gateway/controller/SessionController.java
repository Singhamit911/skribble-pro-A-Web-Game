package com.skribblepro.gateway.controller;

import com.skribblepro.dto.session.CreateSessionRequest;
import com.skribblepro.dto.session.SessionResponse;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/sessions")
public class SessionController {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Duration SESSION_TTL = Duration.ofHours(3);

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public SessionController(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostMapping
    public Mono<SessionResponse> createSession(@RequestBody CreateSessionRequest request) {
        if (request == null || request.name() == null || request.name().trim().length() < 3 || request.name().trim().length() > 20) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be between 3 and 20 characters"));
        }

        if (request.color() == null || !HEX_COLOR.matcher(request.color()).matches()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Color must be a valid hex code (e.g. #FF6B6B)"));
        }

        String name = request.name().trim();
        String color = request.color().toUpperCase();
        String guestId = UUID.randomUUID().toString();
        String sessionToken = UUID.randomUUID().toString();
        String key = "session:" + sessionToken;

        Map<String, String> sessionData = Map.of(
            "guestId", guestId,
            "name", name,
            "color", color
        );

        return redisTemplate.opsForHash().putAll(key, sessionData)
            .then(redisTemplate.expire(key, SESSION_TTL))
            .thenReturn(new SessionResponse(sessionToken, guestId, name, color));
    }

    @DeleteMapping
    public Mono<Void> deleteSession(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Bearer token"));
        }

        String token = authHeader.substring(7).trim();
        String key = "session:" + token;
        return redisTemplate.delete(key).then();
    }
}
