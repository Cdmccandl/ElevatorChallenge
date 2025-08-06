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
        //check if we can execute command based on elevator state
        // Validate current state
        if (!canExecuteCommandInCurrentState(elevator)) {
            log.warn("Cannot open doors - elevator state: {}, busy: {}",
                    elevator.getCurrentState(), elevator.isBusy());
            return;
        }

        if (elevator.isDoorsOpen()) {
            log.debug("Doors are already open at floor {}", elevator.getCurrentFloor());
            return;
        }

        //if we pass these checks we can start the door opening process
        elevator.startDoorOperationTimer();
        log.info("Started door opening process at floor {} ({}ms duration)",
                elevator.getCurrentFloor(), elevator.getDoorOperationTimeMs());

        }

    @Override
    public String getCommandType() {
        return "OPEN DOORS";
    }

    @Override
    public boolean canExecuteCommandInCurrentState(Elevator elevator) {

        ElevatorState currentState = elevator.getCurrentState();

        // Cannot open doors while movement is in progress (floor-by-floor movement)
        if (elevator.isMovementInProgress()) {
            log.error("Cannot open doors: elevator is moving between floors");
            throw new IllegalArgumentException("Cannot open doors: elevator is moving between floors");
        }

        // Cannot open doors if door operation is already in progress
        if (elevator.isDoorOperationInProgress()) {
            log.debug("Cannot open doors: door operation already in progress");
            return false;
        }

        // Cannot open doors if already open
        if (elevator.isDoorsOpen()) {
            log.debug("Cannot open doors: doors are already open");
            return false;
        }

        return currentState == ElevatorState.IDLE || currentState == ElevatorState.DOORS_OPEN;
    }


}
