package com.bluestaq.elevatorChallenge.service.commands;

import com.bluestaq.elevatorChallenge.service.Elevator;
import com.bluestaq.elevatorChallenge.service.ElevatorState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CloseDoorsCommand implements ElevatorCommand {

    @Override
    public void execute(Elevator elevator) {
        // Validate current state
        if (!canExecuteCommandInCurrentState(elevator)) {
            log.warn("Cannot close doors - elevator state: {}, doors open: {}, busy: {}",
                    elevator.getCurrentState(), elevator.isDoorsOpen(), elevator.isBusy());
            return;
        }

        if (!elevator.isDoorsOpen()) {
            log.debug("Doors are already closed at floor {}", elevator.getCurrentFloor());
            return;
        }

        // Start the door closing process
        elevator.startDoorOperationTimer();

        log.info("Started door closing process at floor {} ({}ms duration)",
                elevator.getCurrentFloor(), elevator.getDoorOperationTimeMs());
    }

    @Override
    public String getCommandType() {
        return "CLOSE DOORS";
    }

    /**
     * Check if doors can be closed in the current elevator state
     */
    public boolean canExecuteCommandInCurrentState(Elevator elevator) {
        ElevatorState currentState = elevator.getCurrentState();

        // Cannot close doors while movement is in progress
        if (elevator.isMovementInProgress()) {
            log.debug("Cannot close doors: elevator is moving between floors");
            return false;
        }

        // Cannot close doors if door operation is already in progress
        if (elevator.isDoorOperationInProgress()) {
            log.debug("Cannot close doors: door operation already in progress");
            return false;
        }

        // Can only close doors when they are actually open
        if (!elevator.isDoorsOpen()) {
            log.debug("Cannot close doors: doors are already closed");
            return false;
        }

        // Doors can only be closed when in DOORS_OPEN state
        if (currentState != ElevatorState.DOORS_OPEN) {
            log.debug("Cannot close doors: elevator not in DOORS_OPEN state (current: {})", currentState);
            return false;
        }

        return true;
    }

    /**
     * Get human-readable description of this command
     */
    public String getDescription() {
        return "Close elevator doors and resume operation";
    }


    @Override
    public String toString() {
        return "CloseDoorsCommand{type='" + getCommandType() + "', description='" + getDescription() + "'}";
    }
}
