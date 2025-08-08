package com.bluestaq.elevatorchallenge.service;

import lombok.Getter;

@Getter
public enum ElevatorMovement {

    IDLE("Elevator is stationary and waiting to execute its next command"),
    MOVING("Elevator is traveling between floors"),
    EMERGENCY("Elevator is in emergency stop mode - all operations blocked");


    private final String description;

    ElevatorMovement(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return name() + ": " + description;
    }

}
