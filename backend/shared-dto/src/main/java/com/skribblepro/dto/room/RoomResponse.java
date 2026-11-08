package com.skribblepro.dto.room;

import java.util.List;
import java.util.UUID;

public record RoomResponse(
    UUID id,
    String code,
    String hostGuestId,
    String hostName,
    String status,
    int maxPlayers,
    int rounds,
    int drawTime,
    String category,
    int playerCount,
    List<RoomPlayerDto> players
) {}
