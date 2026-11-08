package com.skribblepro.room.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skribblepro.dto.room.CreateRoomRequest;
import com.skribblepro.dto.room.RoomPlayerDto;
import com.skribblepro.dto.room.RoomResponse;
import com.skribblepro.room.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock
    private RoomService roomService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RoomController roomController = new RoomController(roomService);
        mockMvc = MockMvcBuilders.standaloneSetup(roomController).build();
    }

    @Test
    @DisplayName("POST /rooms: Missing X-Guest-Id header returns 401 Unauthorized")
    void createRoom_MissingHeaders_Returns401() throws Exception {
        mockMvc.perform(post("/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRoomRequest(3, 80, 8, "General"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /rooms: Valid headers and body returns 201 Created with RoomResponse")
    void createRoom_ValidHeaders_Returns201() throws Exception {
        UUID roomId = UUID.randomUUID();
        RoomResponse response = new RoomResponse(
                roomId, "XK92TZ", "host-1", "Sangi", "WAITING",
                8, 3, 80, "General", 1,
                List.of(new RoomPlayerDto("host-1", "Sangi", "#FF6B6B", true))
        );

        when(roomService.createRoom(eq("host-1"), eq("Sangi"), eq("#FF6B6B"), any(CreateRoomRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/rooms")
                        .header("X-Guest-Id", "host-1")
                        .header("X-Guest-Name", "Sangi")
                        .header("X-Guest-Color", "#FF6B6B")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRoomRequest(3, 80, 8, "General"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("XK92TZ"))
                .andExpect(jsonPath("$.hostName").value("Sangi"))
                .andExpect(jsonPath("$.playerCount").value(1));
    }

    @Test
    @DisplayName("GET /rooms/{code}: Returns 200 OK with room information")
    void getRoom_Success_Returns200() throws Exception {
        UUID roomId = UUID.randomUUID();
        RoomResponse response = new RoomResponse(
                roomId, "XK92TZ", "host-1", "Sangi", "WAITING",
                8, 3, 80, "General", 1,
                List.of(new RoomPlayerDto("host-1", "Sangi", "#FF6B6B", true))
        );

        when(roomService.getRoom("XK92TZ")).thenReturn(response);

        mockMvc.perform(get("/rooms/XK92TZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("XK92TZ"))
                .andExpect(jsonPath("$.category").value("General"));
    }

    @Test
    @DisplayName("POST /rooms/{code}/join: Valid headers returns 200 OK")
    void joinRoom_Success_Returns200() throws Exception {
        UUID roomId = UUID.randomUUID();
        RoomResponse response = new RoomResponse(
                roomId, "XK92TZ", "host-1", "Sangi", "WAITING",
                8, 3, 80, "General", 2,
                List.of(
                        new RoomPlayerDto("host-1", "Sangi", "#FF6B6B", true),
                        new RoomPlayerDto("player-2", "Alex", "#4ECDC4", false)
                )
        );

        when(roomService.joinRoom("XK92TZ", "player-2", "Alex", "#4ECDC4")).thenReturn(response);

        mockMvc.perform(post("/rooms/XK92TZ/join")
                        .header("X-Guest-Id", "player-2")
                        .header("X-Guest-Name", "Alex")
                        .header("X-Guest-Color", "#4ECDC4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerCount").value(2));
    }

    @Test
    @DisplayName("DELETE /rooms/{code}/leave: Calls leaveRoom and returns 204 No Content")
    void leaveRoom_Success_Returns204() throws Exception {
        mockMvc.perform(delete("/rooms/XK92TZ/leave")
                        .header("X-Guest-Id", "player-2"))
                .andExpect(status().isNoContent());

        verify(roomService).leaveRoom("XK92TZ", "player-2");
    }
}
