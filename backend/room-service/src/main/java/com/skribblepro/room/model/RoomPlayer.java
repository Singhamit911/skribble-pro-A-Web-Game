package com.skribblepro.room.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "room_players")
public class RoomPlayer {

    @EmbeddedId
    private RoomPlayerId id;

    @Column(name = "guest_name", length = 50, nullable = false)
    private String guestName;

    @Column(name = "color", length = 7, nullable = false)
    private String color;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    public RoomPlayer() {}

    public RoomPlayer(RoomPlayerId id, String guestName, String color) {
        this.id = id;
        this.guestName = guestName;
        this.color = color;
        this.joinedAt = LocalDateTime.now();
    }

    public RoomPlayerId getId() {
        return id;
    }

    public void setId(RoomPlayerId id) {
        this.id = id;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomPlayer that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
