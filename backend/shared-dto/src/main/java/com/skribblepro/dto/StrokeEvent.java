package com.skribblepro.dto;

public record StrokeEvent(
    double x1,
    double y1,
    double x2,
    double y2,
    String color,
    int brushSize,
    String type // "draw" | "erase" | "clear"
) {}
