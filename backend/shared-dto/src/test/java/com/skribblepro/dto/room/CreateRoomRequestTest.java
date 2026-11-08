package com.skribblepro.dto.room;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateRoomRequestTest {

    @Test
    @DisplayName("Should assign sensible defaults when null arguments are provided")
    void shouldAssignDefaultsWhenNull() {
        CreateRoomRequest request = new CreateRoomRequest(null, null, null, null);

        assertThat(request.rounds()).isEqualTo(3);
        assertThat(request.drawTime()).isEqualTo(80);
        assertThat(request.maxPlayers()).isEqualTo(8);
        assertThat(request.category()).isEqualTo("General");
    }

    @Test
    @DisplayName("Should assign sensible defaults when non-positive arguments are provided")
    void shouldAssignDefaultsWhenNonPositive() {
        CreateRoomRequest request = new CreateRoomRequest(0, -10, -1, "   ");

        assertThat(request.rounds()).isEqualTo(3);
        assertThat(request.drawTime()).isEqualTo(80);
        assertThat(request.maxPlayers()).isEqualTo(8);
        assertThat(request.category()).isEqualTo("General");
    }

    @Test
    @DisplayName("Should retain custom values when valid arguments are provided")
    void shouldRetainCustomValues() {
        CreateRoomRequest request = new CreateRoomRequest(5, 60, 12, "Animals");

        assertThat(request.rounds()).isEqualTo(5);
        assertThat(request.drawTime()).isEqualTo(60);
        assertThat(request.maxPlayers()).isEqualTo(12);
        assertThat(request.category()).isEqualTo("Animals");
    }
}

