package com.skribblepro.dto.room;

public record RoomPlayerDto(
    String guestId,
    String name,
    String color,
    boolean isHost
) {}
