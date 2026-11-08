package com.skribblepro.room.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RoomPlayerId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "guest_id", length = 36, nullable = false)
    private String guestId;

    public RoomPlayerId() {}

    public RoomPlayerId(UUID roomId, String guestId) {
        this.roomId = roomId;
        this.guestId = guestId;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomPlayerId that)) return false;
        return Objects.equals(roomId, that.roomId) && Objects.equals(guestId, that.guestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, guestId);
    }
}
