package com.skribblepro.room.repository;

import com.skribblepro.room.model.RoomPlayer;
import com.skribblepro.room.model.RoomPlayerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, RoomPlayerId> {
    List<RoomPlayer> findByIdRoomIdOrderByJoinedAtAsc(UUID roomId);
    Optional<RoomPlayer> findByIdRoomIdAndIdGuestId(UUID roomId, String guestId);
    void deleteByIdRoomIdAndIdGuestId(UUID roomId, String guestId);
    void deleteByIdRoomId(UUID roomId);
}
