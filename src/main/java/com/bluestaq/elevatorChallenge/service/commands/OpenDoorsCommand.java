package com.bluestaq.elevatorChallenge.service.commands;

import com.bluestaq.elevatorChallenge.service.Elevator;
import com.bluestaq.elevatorChallenge.service.ElevatorDirection;
import com.bluestaq.elevatorChallenge.service.ElevatorState;
import lombok.extern.slf4j.Slf4j;

/**
 * A POJO that defines the open doors command on the elevator.
 * This will:
 * 1. Set doors to open state
 * 2. Transition elevator to DOORS_OPEN state
 * 3. Stop any movement (set direction to NONE)
 *
 */
 @Slf4j
 public class OpenDoorsCommand implements ElevatorCommand {

    @Override
    public void execute(Elevator elevator) {
        // Validate that we can execute in current state
        if (!canExecuteCommandInCurrentState(elevator)) {
            log.warn("Cannot open doors - elevator state: {}, moving: {}, busy: {}",
                    elevator.getCurrentState(),
                    elevator.isMovementInProgress(),
                    elevator.isBusy());
            return;
        }

        // Delegate to elevator's door opening method
        boolean started = elevator.startDoorOpening();

        if (started) {
            log.info("Door opening initiated at floor {}", elevator.getCurrentFloor());
        } else {
            log.debug("Door opening not started - elevator reported unable to open");
        }
    }

    @Override
    public String getCommandType() {
        return "OPEN DOORS";
    }

    @Override
    public boolean canExecuteCommandInCurrentState(Elevator elevator) {
        // Cannot open doors while moving
        if (elevator.isMovementInProgress()) {
            log.debug("Cannot open doors: elevator is moving between floors");
            return false;
        }

        // Cannot open if a door operation is already in progress
        if (elevator.isDoorOperationInProgress()) {
            log.debug("Cannot open doors: door operation already in progress");
            return false;
        }

        // Cannot open if doors are already open
        if (elevator.isDoorsOpen()) {
            log.debug("Cannot open doors: doors are already open");
            return false;
        }

        // Can open doors when IDLE or DOORS_OPEN (DOORS_OPEN state means doors are operating)
        ElevatorState currentState = elevator.getCurrentState();
        return currentState == ElevatorState.IDLE || currentState == ElevatorState.DOORS_OPEN;
    }
}
