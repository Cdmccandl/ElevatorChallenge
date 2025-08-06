package com.bluestaq.elevatorChallenge.service.commands;

import com.bluestaq.elevatorChallenge.service.Elevator;
import com.bluestaq.elevatorChallenge.service.ElevatorState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Command to press a floor button and add destination to the elevator queue.
 * Handles destination addition and movement initiation for floor-by-floor travel.
 */
@Slf4j
public class PressButtonCommand implements ElevatorCommand {

    @Getter
    private final int targetFloor;

    public PressButtonCommand(int targetFloor) {
        this.targetFloor = targetFloor;
    }

    @Override
    public void execute(Elevator elevator) {

        // Check if already at target floor
        if (targetFloor == elevator.getCurrentFloor()) {
            log.info("Already at floor {}, ignoring button press", targetFloor);
            return;
        }

        // Validate current state
        if (!canExecuteCommandInCurrentState(elevator)) {
            log.warn("Cannot press button for floor {} - elevator state: {}, busy: {}",
                    targetFloor, elevator.getCurrentState(), elevator.isBusy());
            return;
        }

        // Add to FIFO destination queue
        elevator.addDestination(targetFloor);
        log.info("Button pressed for floor {}. Current queue: {}", targetFloor, elevator.getDestinationList());

        // Start movement if elevator is idle and can start moving
        if (shouldInitiateMovement(elevator)) {
            initiateMovementToNextDestination(elevator);
        }
    }

    @Override
    public String getCommandType() {
        return "PRESS FLOOR BUTTON: " + targetFloor;
    }

    /**
     * Check if button press can be executed in current elevator state
     */
    @Override
    public boolean canExecuteCommandInCurrentState(Elevator elevator) {
        ElevatorState currentState = elevator.getCurrentState();

        // Can press buttons in most operational states
        return currentState == ElevatorState.IDLE ||
                currentState == ElevatorState.MOVING ||
                currentState == ElevatorState.DOORS_OPEN;
    }

    /**
     * Check if movement should be initiated after adding destination
     */
    private boolean shouldInitiateMovement(Elevator elevator) {
        // Only initiate movement if:
        // 1. Elevator is IDLE (not moving, doors closed)
        // 2. Elevator can start movement (not busy with other operations)
        // 3. There are destinations to process
        return elevator.getCurrentState() == ElevatorState.IDLE &&
                elevator.canStartMovement() &&
                elevator.hasDestinations();
    }

    /**
     * Initiate movement to the next destination in FIFO queue
     */
    private void initiateMovementToNextDestination(Elevator elevator) {
        Integer nextDestination = elevator.peekNextDestination();
        if (nextDestination == null) {
            log.debug("No destinations to move to");
            return;
        }

        if (nextDestination.equals(elevator.getCurrentFloor())) {
            log.debug("Next destination {} is current floor, removing from queue", nextDestination);
            elevator.pollNextDestination();
            return;
        }

        // Start floor-by-floor movement to next destination
        elevator.startMovementToFloor(nextDestination);
    }


    /**
     * Get human-readable description of this command
     */
    public String getDescription() {
        return String.format("Press button for floor %d", targetFloor);
    }

    @Override
    public String toString() {
        return String.format("PressButtonCommand{targetFloor=%d, type='%s', description='%s'}",
                targetFloor, getCommandType(), getDescription());
    }
}
