package com.skribblepro.dto;

public record GameStateEvent(
    String phase,       // "PICKING" | "DRAWING" | "REVEAL" | "GAME_OVER"
    int currentRound,
    int totalRounds,
    String drawerName,
    int wordLength,
    String blanks,      // e.g. "_ _ _ _ _"
    String word,        // Only populated during REVEAL phase, otherwise null
    int timeLeft
) {}
