package com.bluestaq.elevatorChallenge.service;

public enum ElevatorState {

    IDLE("Elevator is stationary and waiting for commands"),
    MOVING("Elevator is traveling between floors"),
    DOORS_OPEN("Elevator doors are open for loading/unloading");


    private final String description;

    ElevatorState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + ": " + description;
    }

}
