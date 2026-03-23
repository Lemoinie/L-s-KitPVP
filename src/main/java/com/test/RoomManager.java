package com.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RoomManager {
    private static RoomManager instance;
    private final List<Room> rooms = new ArrayList<>();

    private RoomManager() {}

    public static RoomManager getInstance() {
        if (instance == null) {
            instance = new RoomManager();
        }
        return instance;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public Room createRoom(UUID creatorUuid, RoomType type) {
        if (getRoomByCreator(creatorUuid).isPresent()) {
            return null;
        }
        Room room = new Room(creatorUuid, type);
        rooms.add(room);
        return room;
    }

    public void removeRoom(Room room) {
        rooms.remove(room);
    }

    public Optional<Room> getRoomByPlayer(UUID playerUuid) {
        return rooms.stream()
                .filter(room -> room.containsPlayer(playerUuid))
                .findFirst();
    }

    public Optional<Room> getRoomByCreator(UUID creatorUuid) {
        return rooms.stream()
                .filter(room -> room.getCreatorUuid().equals(creatorUuid))
                .findFirst();
    }
}
