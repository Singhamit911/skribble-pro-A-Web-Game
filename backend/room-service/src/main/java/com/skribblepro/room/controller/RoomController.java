package com.skribblepro.room.controller;

import com.skribblepro.dto.room.CreateRoomRequest;
import com.skribblepro.dto.room.RoomResponse;
import com.skribblepro.room.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @RequestHeader(value = "X-Guest-Name", required = false) String guestName,
            @RequestHeader(value = "X-Guest-Color", required = false) String guestColor,
            @RequestBody(required = false) CreateRoomRequest request) {

        validateGuestHeaders(guestId, guestName);
        CreateRoomRequest safeRequest = request != null ? request : new CreateRoomRequest(3, 80, 8, "General");
        String color = (guestColor != null && !guestColor.isBlank()) ? guestColor : "#FF6B6B";

        log.info("REST POST /rooms: guest '{}' ({}) creating room with category='{}', rounds={}", guestName, guestId, safeRequest.category(), safeRequest.rounds());
        RoomResponse response = roomService.createRoom(guestId, guestName, color, safeRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String code) {
        log.debug("REST GET /rooms/{}", code);
        RoomResponse response = roomService.getRoom(code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<RoomResponse> joinRoom(
            @PathVariable String code,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @RequestHeader(value = "X-Guest-Name", required = false) String guestName,
            @RequestHeader(value = "X-Guest-Color", required = false) String guestColor) {

        validateGuestHeaders(guestId, guestName);
        String color = (guestColor != null && !guestColor.isBlank()) ? guestColor : "#FF6B6B";

        log.info("REST POST /rooms/{}/join: guest '{}' ({}) joining", code, guestName, guestId);
        RoomResponse response = roomService.joinRoom(code, guestId, guestName, color);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{code}/leave")
    public ResponseEntity<Void> leaveRoomDelete(
            @PathVariable String code,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {

        if (guestId == null || guestId.isBlank()) {
            log.warn("REST DELETE /rooms/{}/leave rejected: missing X-Guest-Id", code);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-Guest-Id header");
        }
        log.info("REST DELETE /rooms/{}/leave: guest ({}) leaving", code, guestId);
        roomService.leaveRoom(code, guestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/leave")
    public ResponseEntity<Void> leaveRoomPost(
            @PathVariable String code,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {

        return leaveRoomDelete(code, guestId);
    }

    private void validateGuestHeaders(String guestId, String guestName) {
        if (guestId == null || guestId.isBlank() || guestName == null || guestName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing guest session headers (X-Guest-Id, X-Guest-Name)");
        }
    }
}
