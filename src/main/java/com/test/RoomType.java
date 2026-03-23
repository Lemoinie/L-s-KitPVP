package com.test;

public enum RoomType {
    ONE_V_ONE("1v1", 2),
    TWO_V_TWO("2v2", 4),
    FOUR_V_FOUR("4v4", 8),
    CHAOS("Chaos", 16);

    private final String displayName;
    private final int maxPlayers;

    RoomType(String displayName, int maxPlayers) {
        this.displayName = displayName;
        this.maxPlayers = maxPlayers;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}
