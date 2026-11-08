package com.skribblepro.dto.room;

public record CreateRoomRequest(
    Integer rounds,
    Integer drawTime,
    Integer maxPlayers,
    String category
) {
    public CreateRoomRequest {
        if (rounds == null || rounds <= 0) rounds = 3;
        if (drawTime == null || drawTime <= 0) drawTime = 80;
        if (maxPlayers == null || maxPlayers <= 0) maxPlayers = 8;
        if (category == null || category.isBlank()) category = "General";
    }
}
