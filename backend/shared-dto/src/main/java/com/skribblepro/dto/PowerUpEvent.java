package com.skribblepro.dto;

public record PowerUpEvent(
    String type,        // "FREEZE" | "ERASE" | "FAKE_LINES" | "SHIELD" | "HINT"
    String byGuestId,
    String byName
) {}
