package com.bluestaq.elevatorChallenge.service.commands;

import com.bluestaq.elevatorChallenge.service.Elevator;
import com.bluestaq.elevatorChallenge.service.ElevatorState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CloseDoorsCommand implements ElevatorCommand {

    @Override
    public void execute(Elevator elevator) {
        // Validate that we can execute in current state
        if (!canExecuteCommandInCurrentState(elevator)) {
            log.warn("Cannot close doors - elevator state: {}, doors open: {}, busy: {}",
                    elevator.getCurrentState(),
                    elevator.isDoorsOpen(),
                    elevator.isBusy());
            return;
        }

        // Delegate to elevator's door closing method
        boolean started = elevator.startDoorClosing();

        if (started) {
            log.info("Door closing initiated at floor {}", elevator.getCurrentFloor());
        } else {
            log.debug("Door closing not started - elevator reported unable to close");
        }
    }

    @Override
    public String getCommandType() {
        return "CLOSE DOORS";
    }

    @Override
    public boolean canExecuteCommandInCurrentState(Elevator elevator) {
        // Cannot close doors while moving
        if (elevator.isMovementInProgress()) {
            log.debug("Cannot close doors: elevator is moving between floors");
            return false;
        }

        // Cannot close if a door operation is already in progress
        if (elevator.isDoorOperationInProgress()) {
            log.debug("Cannot close doors: door operation already in progress");
            return false;
        }

        // Cannot close if doors are already closed
        if (!elevator.isDoorsOpen()) {
            log.debug("Cannot close doors: doors are already closed");
            return false;
        }

        // Can only close when in DOORS_OPEN state with doors actually open
        return elevator.getCurrentState() == ElevatorState.DOORS_OPEN;
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
