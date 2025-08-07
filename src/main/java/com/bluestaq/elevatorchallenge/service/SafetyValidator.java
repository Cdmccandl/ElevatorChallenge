package com.bluestaq.elevatorchallenge.service;

import org.springframework.stereotype.Component;

@Component
public class SafetyValidator {

    /**
     * Simple check to show if opening the doors is a valid option
     */
    public boolean canOpenDoors(ElevatorState state) {
        //if elevator doors are closed and idle
        if(state.getCurrentMovementState() != ElevatorMovement.IDLE) {
            return false; //we cannot open doors while in motion
        }

        return switch (state.getCurrentDoorState()) {
            case CLOSED, CLOSING, OPENING -> true;
            case OPEN -> false; //already open
        };

    }

    public boolean canCloseDoors(ElevatorState state) {
        //if elevator is in motion
        if (state.getCurrentMovementState() != ElevatorMovement.IDLE) {
            return false; //we cannot open doors while in motion
        }

        return switch (state.getCurrentDoorState()) {
            case OPEN, OPENING, CLOSING -> true;
            case CLOSED -> false; //already closed
        };
    }
}
