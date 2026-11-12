package com.skribblepro.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class SessionValidationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SessionValidationFilter.class);
    private static final Duration SESSION_TTL = Duration.ofHours(1);

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public SessionValidationFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int getOrder() {
        return -1; // Highest precedence before routing
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // Allow public/handshake endpoints and CORS preflight without validation
        if (method == HttpMethod.OPTIONS
                || (path.equals("/sessions") && method == HttpMethod.POST)
                || path.startsWith("/actuator")
                || path.startsWith("/rooms-actuator")
                || path.startsWith("/games-actuator")
                || path.startsWith("/words-actuator")
                || path.startsWith("/ws")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Unauthorized request blocked for path [{}]: missing Bearer Authorization header", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            log.warn("Unauthorized request blocked for path [{}]: empty Bearer token", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String key = "session:" + token;

        return redisTemplate.<String, String>opsForHash().entries(key)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(sessionData -> {
                    if (sessionData == null || sessionData.isEmpty() || !sessionData.containsKey("guestId")) {
                        log.warn("Unauthorized request blocked for path [{}]: session token not found or expired in Redis", path);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }

                    String guestId = sessionData.get("guestId");
                    String name = sessionData.get("name");
                    log.debug("Session validated for guest '{}' ({}) -> forwarding to [{}]", name, guestId, path);

                    return redisTemplate.expire(key, SESSION_TTL)
                            .then(Mono.defer(() -> {
                                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                        .header("X-Guest-Id", guestId)
                                        .header("X-Guest-Name", name)
                                        .header("X-Guest-Color", sessionData.get("color"))
                                        .build();

                                return chain.filter(exchange.mutate().request(mutatedRequest).build());
                            }));
                });
    }
}
