package com.bluestaq.elevatorchallenge.service.commands;


import com.bluestaq.elevatorchallenge.service.ElevatorDestinationManager;
import com.bluestaq.elevatorchallenge.service.ElevatorMovement;
import com.bluestaq.elevatorchallenge.service.ElevatorState;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Command to press a floor button within the elevator and add destination to the elevator queue.
 * Handles destination addition and movement initiation for floor-by-floor travel.
 */
@Slf4j
@Component
public class PressButtonCommand implements ElevatorCommand {

    @Getter
    @Setter
    private int targetFloor;

    @Autowired
    private ElevatorDestinationManager destinationManager;



    @Override
    public boolean executeCommand(ElevatorState state) {
        log.info("Executing press button command for floor {}", targetFloor);

        //validate we can execute a floor command in the current state
        if (!canExecuteCommand(state)) {
            throw new IllegalArgumentException("Cannot press floor button " + targetFloor + " in current elevator state");
        }

        // Don't add to destinations list if already at target floor
        if (targetFloor == state.getCurrentFloor()) {
            log.info("Already at floor {}, movement action ignored", targetFloor);
            return true;
        }

        //add the destination if it is valid
        boolean destinationAdded = destinationManager.addDestination(targetFloor, state);


        if (destinationAdded) {
            log.info("Floor {} button pressed successfully. Current Destinations: {}", targetFloor, destinationManager.getAllDestinations());
            return true;
        } else {
            log.error("Failed to add floor {} to destinations", targetFloor);
            return false;
        }
    }

    @Override
    public boolean canExecuteCommand(ElevatorState elevatorState) {
        // Validate floor is in range
        if (!elevatorState.isValidFloor(targetFloor)) {
            log.warn("Invalid floor {}: must be between {} and {}",
                    targetFloor, elevatorState.getMinFloor(), elevatorState.getMaxFloor());
            return false;
        }

        // Can press buttons in most states
        ElevatorMovement currentState = elevatorState.getCurrentMovementState();

        return currentState == ElevatorMovement.IDLE ||
                currentState == ElevatorMovement.MOVING;
    }

}
