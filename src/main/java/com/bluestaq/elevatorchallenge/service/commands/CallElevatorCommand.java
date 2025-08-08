package com.bluestaq.elevatorchallenge.service.commands;

import com.bluestaq.elevatorchallenge.service.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Command to press a direction button on specified floor. handles elevator queue logic based
 * on the passed in direction
 */
@Component
@Slf4j
public class CallElevatorCommand implements ElevatorCommand {

    @Autowired
    private ElevatorDestinationManager destinationManager;

    @Getter
    @Setter
    private int targetFloor;

    @Getter
    @Setter
    private ElevatorDirection requestedDirection;


    @Override
    public boolean executeCommand(ElevatorState state) {
        log.info("Executing Call Elevator request: Floor {} {}", targetFloor, requestedDirection);

        //validate we can execute a floor command in the current state
        if (!canExecuteCommand(state)) {
            throw new IllegalArgumentException("Cannot press floor button " + targetFloor + " in current elevator state");
        }

        // Don't add to destinations list if already at target floor
        if (targetFloor == state.getCurrentFloor()) {
            log.info("Already at floor {}, movement action ignored", targetFloor);
            return true;
        }

        // Add to destination manager using explicit direction
        boolean destinationAdded = destinationManager.addFloorRequestWithDirection(targetFloor, requestedDirection, state);

        if (destinationAdded) {
            log.info("Floor {} button pressed successfully. Current Destinations: {}", targetFloor, destinationManager.getAllDestinations());
            return true;
        } else {
            log.error("Failed to add floor {} to destinations", targetFloor);
            return false;
        }
    }

    @Override
    public boolean canExecuteCommand(ElevatorState state) {
        // Validate floor is in range
        if (!state.isValidFloor(targetFloor)) {
            log.warn("Invalid floor {}: must be between {} and {}",
                    targetFloor, state.getMinFloor(), state.getMaxFloor());
            return false;
        }
        // Validate direction makes sense for floor
        if (!isValidDirectionForCallingFloor(state)) {
            return false;
        }
        // Can press buttons in most states
        ElevatorMovement currentState = state.getCurrentMovementState();

        return currentState == ElevatorMovement.IDLE ||
                currentState == ElevatorMovement.MOVING;
    }

    /**
     * Method to check edge cases for pressing a call button on the top and bottom floors.
     */
    private boolean isValidDirectionForCallingFloor(ElevatorState elevatorState) {

        // Ground floor can only request UP
        if (targetFloor == elevatorState.getMinFloor() && requestedDirection == ElevatorDirection.DOWN) {
            log.warn("Cannot request DOWN from ground floor {}", targetFloor);
            return false;
        }

        // Top floor can only request DOWN
        if (targetFloor == elevatorState.getMaxFloor() && requestedDirection == ElevatorDirection.UP) {
            log.warn("Cannot request UP from top floor {}", targetFloor);
            return false;
        }

        // NONE direction not allowed
        if (requestedDirection == ElevatorDirection.NONE) {
            throw new IllegalArgumentException("Cannot request direction NONE from elevator");
        }

        return true;
    }

}
