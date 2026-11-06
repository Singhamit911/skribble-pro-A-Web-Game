package com.skribblepro.dto.session;

public record SessionResponse(
    String sessionToken,
    String guestId,
    String name,
    String color
) {}
