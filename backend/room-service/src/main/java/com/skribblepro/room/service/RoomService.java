package com.skribblepro.room.service;

import com.skribblepro.dto.room.CreateRoomRequest;
import com.skribblepro.dto.room.RoomPlayerDto;
import com.skribblepro.dto.room.RoomResponse;
import com.skribblepro.room.model.Room;
import com.skribblepro.room.model.RoomPlayer;
import com.skribblepro.room.model.RoomPlayerId;
import com.skribblepro.room.repository.RoomPlayerRepository;
import com.skribblepro.room.repository.RoomRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RoomService {

    private static final Duration ROOM_TTL = Duration.ofHours(4);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Omit confusing letters: I, O, 0, 1
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final StringRedisTemplate redisTemplate;

    public RoomService(RoomRepository roomRepository,
                       RoomPlayerRepository roomPlayerRepository,
                       StringRedisTemplate redisTemplate) {
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public RoomResponse createRoom(String hostGuestId, String hostName, String hostColor, CreateRoomRequest request) {
        String code = generateUniqueRoomCode();
        UUID roomId = UUID.randomUUID();

        Room room = new Room(
                roomId,
                code,
                hostGuestId,
                hostName,
                request.rounds(),
                request.drawTime(),
                request.maxPlayers(),
                request.category()
        );
        roomRepository.save(room);

        RoomPlayer hostPlayer = new RoomPlayer(new RoomPlayerId(roomId, hostGuestId), hostName, hostColor);
        roomPlayerRepository.save(hostPlayer);

        // Redis: cache room metadata
        String roomKey = "room:" + code;
        String playersKey = "room:" + code + ":players";

        Map<String, String> roomData = Map.of(
                "id", roomId.toString(),
                "code", code,
                "hostGuestId", hostGuestId,
                "hostName", hostName,
                "status", "WAITING",
                "maxPlayers", String.valueOf(room.getMaxPlayers()),
                "rounds", String.valueOf(room.getRounds()),
                "drawTime", String.valueOf(room.getDrawTime()),
                "category", room.getCategory(),
                "playerCount", "1"
        );

        redisTemplate.opsForHash().putAll(roomKey, roomData);
        redisTemplate.expire(roomKey, ROOM_TTL);

        // Redis: add host to players hash
        redisTemplate.opsForHash().put(playersKey, hostGuestId, hostName + "|" + hostColor);
        redisTemplate.expire(playersKey, ROOM_TTL);

        List<RoomPlayerDto> players = List.of(new RoomPlayerDto(hostGuestId, hostName, hostColor, true));
        return new RoomResponse(
                roomId,
                code,
                hostGuestId,
                hostName,
                "WAITING",
                room.getMaxPlayers(),
                room.getRounds(),
                room.getDrawTime(),
                room.getCategory(),
                1,
                players
        );
    }

    public RoomResponse getRoom(String rawCode) {
        String code = normalizeCode(rawCode);
        String roomKey = "room:" + code;
        String playersKey = "room:" + code + ":players";

        Map<Object, Object> roomData = redisTemplate.opsForHash().entries(roomKey);

        if (roomData == null || roomData.isEmpty()) {
            // Cache miss: try rehydrating from PostgreSQL
            Room room = roomRepository.findByCode(code)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found: " + code));
            return rehydrateAndGet(room);
        }

        return buildResponseFromRedis(code, roomData, playersKey);
    }

    @Transactional
    public RoomResponse joinRoom(String rawCode, String guestId, String name, String color) {
        String code = normalizeCode(rawCode);
        String roomKey = "room:" + code;
        String playersKey = "room:" + code + ":players";

        Map<Object, Object> roomData = redisTemplate.opsForHash().entries(roomKey);
        Room room;

        if (roomData == null || roomData.isEmpty()) {
            room = roomRepository.findByCode(code)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found: " + code));
            rehydrateCache(room);
            roomData = redisTemplate.opsForHash().entries(roomKey);
        } else {
            UUID roomId = UUID.fromString((String) roomData.get("id"));
            room = roomRepository.findById(roomId).orElse(null);
        }

        String status = (String) roomData.get("status");
        if (!"WAITING".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot join room. Game status is " + status);
        }

        int maxPlayers = Integer.parseInt((String) roomData.getOrDefault("maxPlayers", "8"));
        Map<Object, Object> currentPlayers = redisTemplate.opsForHash().entries(playersKey);

        // Check if player is already present
        if (currentPlayers.containsKey(guestId)) {
            return buildResponseFromRedis(code, roomData, playersKey);
        }

        if (currentPlayers.size() >= maxPlayers) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room is full (max " + maxPlayers + " players)");
        }

        // Persist to PostgreSQL
        UUID roomId = UUID.fromString((String) roomData.get("id"));
        RoomPlayer roomPlayer = new RoomPlayer(new RoomPlayerId(roomId, guestId), name, color);
        roomPlayerRepository.save(roomPlayer);

        // Update Redis
        redisTemplate.opsForHash().put(playersKey, guestId, name + "|" + color);
        redisTemplate.expire(playersKey, ROOM_TTL);

        int updatedCount = currentPlayers.size() + 1;
        redisTemplate.opsForHash().put(roomKey, "playerCount", String.valueOf(updatedCount));
        redisTemplate.expire(roomKey, ROOM_TTL);

        roomData.put("playerCount", String.valueOf(updatedCount));
        return buildResponseFromRedis(code, roomData, playersKey);
    }

    @Transactional
    public void leaveRoom(String rawCode, String guestId) {
        String code = normalizeCode(rawCode);
        String roomKey = "room:" + code;
        String playersKey = "room:" + code + ":players";

        Map<Object, Object> roomData = redisTemplate.opsForHash().entries(roomKey);
        if (roomData == null || roomData.isEmpty()) {
            return;
        }

        UUID roomId = UUID.fromString((String) roomData.get("id"));
        String hostGuestId = (String) roomData.get("hostGuestId");

        // Delete from DB and Redis
        roomPlayerRepository.deleteByIdRoomIdAndIdGuestId(roomId, guestId);
        redisTemplate.opsForHash().delete(playersKey, guestId);

        Map<Object, Object> remainingPlayers = redisTemplate.opsForHash().entries(playersKey);

        if (remainingPlayers.isEmpty()) {
            // Room is empty, finish it
            redisTemplate.delete(roomKey);
            redisTemplate.delete(playersKey);

            roomRepository.findById(roomId).ifPresent(r -> {
                r.setStatus("FINISHED");
                roomRepository.save(r);
            });
        } else {
            int newCount = remainingPlayers.size();
            redisTemplate.opsForHash().put(roomKey, "playerCount", String.valueOf(newCount));

            // If the host left, elect the next remaining player as new host
            if (guestId.equals(hostGuestId)) {
                Map.Entry<Object, Object> nextHost = remainingPlayers.entrySet().iterator().next();
                String newHostId = (String) nextHost.getKey();
                String newHostName = ((String) nextHost.getValue()).split("\\|")[0];

                redisTemplate.opsForHash().put(roomKey, "hostGuestId", newHostId);
                redisTemplate.opsForHash().put(roomKey, "hostName", newHostName);

                roomRepository.findById(roomId).ifPresent(r -> {
                    r.setHostGuestId(newHostId);
                    r.setHostName(newHostName);
                    roomRepository.save(r);
                });
            }

            redisTemplate.expire(roomKey, ROOM_TTL);
            redisTemplate.expire(playersKey, ROOM_TTL);
        }
    }

    private String generateUniqueRoomCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!roomRepository.existsByCode(code) && Boolean.FALSE.equals(redisTemplate.hasKey("room:" + code))) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique room code. Please try again.");
    }

    private String normalizeCode(String code) {
        if (code == null || code.trim().length() != 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room code must be exactly 6 characters");
        }
        return code.trim().toUpperCase();
    }

    private RoomResponse buildResponseFromRedis(String code, Map<Object, Object> roomData, String playersKey) {
        String hostGuestId = (String) roomData.get("hostGuestId");
        Map<Object, Object> playerEntries = redisTemplate.opsForHash().entries(playersKey);

        List<RoomPlayerDto> players = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : playerEntries.entrySet()) {
            String guestId = (String) entry.getKey();
            String val = (String) entry.getValue();
            String[] parts = val.split("\\|", 2);
            String name = parts.length > 0 ? parts[0] : "Player";
            String color = parts.length > 1 ? parts[1] : "#FF6B6B";
            boolean isHost = guestId.equals(hostGuestId);
            players.add(new RoomPlayerDto(guestId, name, color, isHost));
        }

        return new RoomResponse(
                UUID.fromString((String) roomData.get("id")),
                code,
                hostGuestId,
                (String) roomData.get("hostName"),
                (String) roomData.get("status"),
                Integer.parseInt((String) roomData.getOrDefault("maxPlayers", "8")),
                Integer.parseInt((String) roomData.getOrDefault("rounds", "3")),
                Integer.parseInt((String) roomData.getOrDefault("drawTime", "80")),
                (String) roomData.getOrDefault("category", "General"),
                players.size(),
                players
        );
    }

    private RoomResponse rehydrateAndGet(Room room) {
        rehydrateCache(room);
        String code = room.getCode();
        String roomKey = "room:" + code;
        String playersKey = "room:" + code + ":players";
        Map<Object, Object> roomData = redisTemplate.opsForHash().entries(roomKey);
        return buildResponseFromRedis(code, roomData, playersKey);
    }

    private void rehydrateCache(Room room) {
        String code = room.getCode();
        String roomKey = "room:" + code;
        String playersKey = "room:" + code + ":players";

        List<RoomPlayer> dbPlayers = roomPlayerRepository.findByIdRoomIdOrderByJoinedAtAsc(room.getId());

        Map<String, String> roomData = Map.of(
                "id", room.getId().toString(),
                "code", code,
                "hostGuestId", room.getHostGuestId(),
                "hostName", room.getHostName(),
                "status", room.getStatus(),
                "maxPlayers", String.valueOf(room.getMaxPlayers()),
                "rounds", String.valueOf(room.getRounds()),
                "drawTime", String.valueOf(room.getDrawTime()),
                "category", room.getCategory(),
                "playerCount", String.valueOf(dbPlayers.size())
        );

        redisTemplate.opsForHash().putAll(roomKey, roomData);
        redisTemplate.expire(roomKey, ROOM_TTL);

        for (RoomPlayer p : dbPlayers) {
            redisTemplate.opsForHash().put(playersKey, p.getId().getGuestId(), p.getGuestName() + "|" + p.getColor());
        }
        redisTemplate.expire(playersKey, ROOM_TTL);
    }
}
