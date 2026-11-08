package com.skribblepro.room.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", length = 6, nullable = false, unique = true)
    private String code;

    @Column(name = "host_guest_id", length = 36, nullable = false)
    private String hostGuestId;

    @Column(name = "host_name", length = 50, nullable = false)
    private String hostName;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "WAITING";

    @Column(name = "max_players", nullable = false)
    private int maxPlayers = 8;

    @Column(name = "rounds", nullable = false)
    private int rounds = 3;

    @Column(name = "draw_time", nullable = false)
    private int drawTime = 80;

    @Column(name = "category", length = 50, nullable = false)
    private String category = "General";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Room() {}

    public Room(UUID id, String code, String hostGuestId, String hostName, int rounds, int drawTime, int maxPlayers, String category) {
        this.id = id != null ? id : UUID.randomUUID();
        this.code = code;
        this.hostGuestId = hostGuestId;
        this.hostName = hostName;
        this.rounds = rounds;
        this.drawTime = drawTime;
        this.maxPlayers = maxPlayers;
        this.category = (category != null && !category.isBlank()) ? category : "General";
        this.status = "WAITING";
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getHostGuestId() {
        return hostGuestId;
    }

    public void setHostGuestId(String hostGuestId) {
        this.hostGuestId = hostGuestId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    public int getDrawTime() {
        return drawTime;
    }

    public void setDrawTime(int drawTime) {
        this.drawTime = drawTime;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room room)) return false;
        return Objects.equals(id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
