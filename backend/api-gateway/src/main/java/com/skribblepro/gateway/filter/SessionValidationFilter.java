package com.skribblepro.gateway.filter;

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
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String key = "session:" + token;

        return redisTemplate.<String, String>opsForHash().entries(key)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(sessionData -> {
                    if (sessionData == null || sessionData.isEmpty() || !sessionData.containsKey("guestId")) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }

                    return redisTemplate.expire(key, SESSION_TTL)
                            .then(Mono.defer(() -> {
                                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                        .header("X-Guest-Id", sessionData.get("guestId"))
                                        .header("X-Guest-Name", sessionData.get("name"))
                                        .header("X-Guest-Color", sessionData.get("color"))
                                        .build();

                                return chain.filter(exchange.mutate().request(mutatedRequest).build());
                            }));
                });
    }
}
