package com.bluestaq.elevatorchallenge.service;

public enum ElevatorDirection {
    UP(1, "Moving upward"),
    DOWN(-1, "Moving downward"),
    NONE(0, "Stationary");

    private final int value;
    private final String description;

    ElevatorDirection(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    // Helper methods for direction logic
    public boolean isMoving() {
        return this != NONE;
    }

    public ElevatorDirection opposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case NONE -> NONE;
        };
    }

    // Calculate direction between two floors
    public static ElevatorDirection between(int fromFloor, int toFloor) {
        if (fromFloor < toFloor) return UP;
        if (fromFloor > toFloor) return DOWN;
        return NONE;
    }

    // Check if a floor is in the direction of travel
    public boolean isTowards(int currentFloor, int targetFloor) {
        return switch (this) {
            case UP -> targetFloor > currentFloor;
            case DOWN -> targetFloor < currentFloor;
            case NONE -> true; // Can go anywhere when stationary
        };
    }

    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}
