package com.skribblepro.room.service;

import com.skribblepro.dto.room.CreateRoomRequest;
import com.skribblepro.dto.room.RoomResponse;
import com.skribblepro.room.model.Room;
import com.skribblepro.room.model.RoomPlayer;
import com.skribblepro.room.model.RoomPlayerId;
import com.skribblepro.room.repository.RoomPlayerRepository;
import com.skribblepro.room.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomPlayerRepository roomPlayerRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(roomRepository, roomPlayerRepository, redisTemplate);
    }

    @Test
    @DisplayName("createRoom: Persists Room and host Player in DB and caches in Redis with 4h TTL")
    void createRoom_Success() {
        when(roomRepository.existsByCode(anyString())).thenReturn(false);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        CreateRoomRequest request = new CreateRoomRequest(5, 60, 8, "Animals");
        String hostId = "host-uuid-1";
        String hostName = "Sangi";
        String hostColor = "#FF6B6B";

        RoomResponse response = roomService.createRoom(hostId, hostName, hostColor, request);

        assertThat(response).isNotNull();
        assertThat(response.code()).hasSize(6);
        assertThat(response.hostGuestId()).isEqualTo(hostId);
        assertThat(response.hostName()).isEqualTo(hostName);
        assertThat(response.status()).isEqualTo("WAITING");
        assertThat(response.rounds()).isEqualTo(5);
        assertThat(response.drawTime()).isEqualTo(60);
        assertThat(response.maxPlayers()).isEqualTo(8);
        assertThat(response.category()).isEqualTo("Animals");
        assertThat(response.playerCount()).isEqualTo(1);
        assertThat(response.players()).hasSize(1);
        assertThat(response.players().get(0).isHost()).isTrue();

        verify(roomRepository).save(any(Room.class));
        verify(roomPlayerRepository).save(any(RoomPlayer.class));
        verify(hashOperations).putAll(eq("room:" + response.code()), anyMap());
        verify(hashOperations).put(eq("room:" + response.code() + ":players"), eq(hostId), eq("Sangi|#FF6B6B"));
        verify(redisTemplate).expire(eq("room:" + response.code()), eq(Duration.ofHours(4)));
    }

    @Test
    @DisplayName("getRoom: Cache Hit reads directly from Redis without querying PostgreSQL")
    void getRoom_CacheHit_ReadsFromRedis() {
        String code = "XK92TZ";
        String roomKey = "room:" + code;
        String playersKey = "room:" + code + ":players";
        UUID roomId = UUID.randomUUID();

        Map<Object, Object> roomData = Map.of(
                "id", roomId.toString(),
                "code", code,
                "hostGuestId", "host-1",
                "hostName", "Sangi",
                "status", "WAITING",
                "maxPlayers", "8",
                "rounds", "3",
                "drawTime", "80",
                "category", "General",
                "playerCount", "1"
        );

        Map<Object, Object> playersData = Map.of(
                "host-1", "Sangi|#FF6B6B"
        );

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(roomKey)).thenReturn(roomData);
        when(hashOperations.entries(playersKey)).thenReturn(playersData);

        RoomResponse response = roomService.getRoom(code);

        assertThat(response.code()).isEqualTo(code);
        assertThat(response.hostName()).isEqualTo("Sangi");
        assertThat(response.players()).hasSize(1);
        assertThat(response.players().get(0).isHost()).isTrue();

        verify(roomRepository, never()).findByCode(anyString());
    }

    @Test
    @DisplayName("getRoom: Cache Miss rehydrates Redis from PostgreSQL")
    void getRoom_CacheMiss_RehydratesFromPostgres() {
        String code = "XK92TZ";
        UUID roomId = UUID.randomUUID();
        Room room = new Room(roomId, code, "host-1", "Sangi", 3, 80, 8, "General");
        RoomPlayer player = new RoomPlayer(new RoomPlayerId(roomId, "host-1"), "Sangi", "#FF6B6B");

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        // Initial check returns empty (cache miss)
        when(hashOperations.entries("room:" + code)).thenReturn(Map.of())
                .thenReturn(Map.of(
                        "id", roomId.toString(),
                        "code", code,
                        "hostGuestId", "host-1",
                        "hostName", "Sangi",
                        "status", "WAITING",
                        "maxPlayers", "8",
                        "rounds", "3",
                        "drawTime", "80",
                        "category", "General",
                        "playerCount", "1"
                ));

        when(hashOperations.entries("room:" + code + ":players")).thenReturn(Map.of("host-1", "Sangi|#FF6B6B"));
        when(roomRepository.findByCode(code)).thenReturn(Optional.of(room));
        when(roomPlayerRepository.findByIdRoomIdOrderByJoinedAtAsc(roomId)).thenReturn(List.of(player));

        RoomResponse response = roomService.getRoom(code);

        assertThat(response.code()).isEqualTo(code);
        verify(roomRepository).findByCode(code);
        verify(hashOperations).putAll(eq("room:" + code), anyMap());
        verify(hashOperations).put(eq("room:" + code + ":players"), eq("host-1"), eq("Sangi|#FF6B6B"));
    }

    @Test
    @DisplayName("getRoom: Throws 404 Not Found when room does not exist in Redis or DB")
    void getRoom_NotFound_ThrowsNotFound() {
        String code = "NOTFND";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("room:" + code)).thenReturn(Map.of());
        when(roomRepository.findByCode(code)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoom(code))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Room not found");
    }

    @Test
    @DisplayName("joinRoom: Adds player to DB and Redis hash when room has capacity")
    void joinRoom_Success() {
        String code = "XK92TZ";
        UUID roomId = UUID.randomUUID();

        Map<Object, Object> roomData = new HashMap<>(Map.of(
                "id", roomId.toString(),
                "code", code,
                "hostGuestId", "host-1",
                "hostName", "Sangi",
                "status", "WAITING",
                "maxPlayers", "8",
                "rounds", "3",
                "drawTime", "80",
                "category", "General",
                "playerCount", "1"
        ));

        Map<Object, Object> currentPlayers = Map.of(
                "host-1", "Sangi|#FF6B6B"
        );

        Map<Object, Object> updatedPlayers = Map.of(
                "host-1", "Sangi|#FF6B6B",
                "player-2", "Alex|#4ECDC4"
        );

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("room:" + code)).thenReturn(roomData);
        when(hashOperations.entries("room:" + code + ":players"))
                .thenReturn(currentPlayers)
                .thenReturn(updatedPlayers);

        RoomResponse response = roomService.joinRoom(code, "player-2", "Alex", "#4ECDC4");

        assertThat(response.playerCount()).isEqualTo(2);
        verify(roomPlayerRepository).save(any(RoomPlayer.class));
        verify(hashOperations).put("room:" + code + ":players", "player-2", "Alex|#4ECDC4");
        verify(hashOperations).put("room:" + code, "playerCount", "2");
    }

    @Test
    @DisplayName("joinRoom: Throws 400 Bad Request when room is full")
    void joinRoom_RoomFull_ThrowsBadRequest() {
        String code = "XK92TZ";
        UUID roomId = UUID.randomUUID();

        Map<Object, Object> roomData = Map.of(
                "id", roomId.toString(),
                "code", code,
                "status", "WAITING",
                "maxPlayers", "2"
        );

        Map<Object, Object> currentPlayers = Map.of(
                "p1", "Player1|#FF6B6B",
                "p2", "Player2|#4ECDC4"
        );

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("room:" + code)).thenReturn(roomData);
        when(hashOperations.entries("room:" + code + ":players")).thenReturn(currentPlayers);

        assertThatThrownBy(() -> roomService.joinRoom(code, "p3", "Player3", "#FED766"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Room is full");

        verify(roomPlayerRepository, never()).save(any());
    }

    @Test
    @DisplayName("joinRoom: Throws 400 Bad Request when game is already in progress")
    void joinRoom_GameInProgress_ThrowsBadRequest() {
        String code = "XK92TZ";
        UUID roomId = UUID.randomUUID();

        Map<Object, Object> roomData = Map.of(
                "id", roomId.toString(),
                "code", code,
                "status", "IN_PROGRESS",
                "maxPlayers", "8"
        );

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("room:" + code)).thenReturn(roomData);

        assertThatThrownBy(() -> roomService.joinRoom(code, "p2", "Alex", "#4ECDC4"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Game status is IN_PROGRESS");
    }

    @Test
    @DisplayName("joinRoom: Re-joining an already joined room is idempotent")
    void joinRoom_AlreadyJoined_IsIdempotent() {
        String code = "XK92TZ";
        UUID roomId = UUID.randomUUID();

        Map<Object, Object> roomData = Map.of(
                "id", roomId.toString(),
                "code", code,
                "hostGuestId", "host-1",
                "hostName", "Sangi",
                "status", "WAITING",
                "maxPlayers", "8"
        );

        Map<Object, Object> currentPlayers = Map.of(
                "host-1", "Sangi|#FF6B6B",
                "player-2", "Alex|#4ECDC4"
        );

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("room:" + code)).thenReturn(roomData);
        when(hashOperations.entries("room:" + code + ":players")).thenReturn(currentPlayers);

        RoomResponse response = roomService.joinRoom(code, "player-2", "Alex", "#4ECDC4");

        assertThat(response).isNotNull();
        verify(roomPlayerRepository, never()).save(any());
    }

    @Test
    @DisplayName("leaveRoom: Regular player leaves; updates playerCount and retains host")
    void leaveRoom_RegularPlayerLeaves_UpdatesCount() {
        String code = "XK92TZ";
        UUID roomId = UUID.randomUUID();

        Map<Object, Object> roomData = Map.of(
                "id", roomId.toString(),
                "hostGuestId", "host-1"
        );

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("room:" + code)).thenReturn(roomData);
        when(hashOperations.entries("room:" + code + ":players")).thenReturn(Map.of(
                "host-1", "Sangi|#FF6B6B"
        ));

        roomService.leaveRoom(code, "player-2");

        verify(roomPlayerRepository).deleteByIdRoomIdAndIdGuestId(roomId, "player-2");
        verify(hashOperations).delete("room:" + code + ":players", "player-2");
        verify(hashOperations).put("room:" + code, "playerCount", "1");
    }

    @Test
    @DisplayName("leaveRoom: Host leaves; dynamically promotes next player as new host")
    void leaveRoom_HostLeaves_ElectsNewHost() {
        String code = "XK92TZ";
        UUID roomId = UUID.randomUUID();
        Room room = new Room(roomId, code, "host-1", "Sangi", 3, 80, 8, "General");

        Map<Object, Object> roomData = Map.of(
                "id", roomId.toString(),
                "hostGuestId", "host-1"
        );

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("room:" + code)).thenReturn(roomData);
        when(hashOperations.entries("room:" + code + ":players")).thenReturn(Map.of(
                "player-2", "Alex|#4ECDC4"
        ));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        roomService.leaveRoom(code, "host-1");

        verify(hashOperations).put("room:" + code, "hostGuestId", "player-2");
        verify(hashOperations).put("room:" + code, "hostName", "Alex");
        verify(roomRepository).save(any(Room.class));
        assertThat(room.getHostGuestId()).isEqualTo("player-2");
        assertThat(room.getHostName()).isEqualTo("Alex");
    }

    @Test
    @DisplayName("leaveRoom: Last player leaves; finishes room and deletes Redis keys")
    void leaveRoom_LastPlayerLeaves_FinishesRoom() {
        String code = "XK92TZ";
        UUID roomId = UUID.randomUUID();
        Room room = new Room(roomId, code, "host-1", "Sangi", 3, 80, 8, "General");

        Map<Object, Object> roomData = Map.of(
                "id", roomId.toString(),
                "hostGuestId", "host-1"
        );

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("room:" + code)).thenReturn(roomData);
        when(hashOperations.entries("room:" + code + ":players")).thenReturn(Map.of());
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        roomService.leaveRoom(code, "host-1");

        verify(redisTemplate).delete("room:" + code);
        verify(redisTemplate).delete("room:" + code + ":players");
        assertThat(room.getStatus()).isEqualTo("FINISHED");
        verify(roomRepository).save(room);
    }
}
