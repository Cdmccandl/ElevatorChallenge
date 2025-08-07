package com.bluestaq.elevatorChallenge.service;


import com.bluestaq.elevatorChallenge.dto.ElevatorDTO;
import com.bluestaq.elevatorChallenge.service.commands.CloseDoorsCommand;
import com.bluestaq.elevatorChallenge.service.commands.ElevatorCommand;
import com.bluestaq.elevatorChallenge.service.commands.OpenDoorsCommand;
import com.bluestaq.elevatorChallenge.service.commands.PressButtonCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ElevatorService {

    @Autowired
    private ElevatorState elevatorState;

    @Autowired
    private OpenDoorsCommand openDoorsCommand;

    @Autowired
    private CloseDoorsCommand closeDoorsCommand;

    @Autowired
    private PressButtonCommand pressButtonCommand;

    @Autowired
    private ElevatorDestinationManager destinationManager;

    // ==================== Rest request handling ====================

    /**
     * Handle open doors request from REST controller.
     * Executes immediately if conditions are valid.
     */
    public void openDoors() {
        log.info("REST request: Open doors at floor {}", elevatorState.getCurrentFloor());

        try {
            // Use strategy to execute command outside of elevator logic loop
            openDoorsCommand.executeCommand(elevatorState);
        } catch (IllegalArgumentException e) {
            log.error("Cannot open doors: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Handle close doors request from REST controller.
     * Executes immediately if conditions are valid.
     */
    public void closeDoors() {
        log.info("REST request: Close doors at floor {}", elevatorState.getCurrentFloor());

        try {
            // Use strategy to execute command outside of elevator logic loop
            closeDoorsCommand.executeCommand(elevatorState);
        } catch (IllegalArgumentException e) {
            log.error("Cannot close doors: {}", e.getMessage());
            // Let GlobalExceptionHandler handle the HTTP response code for this error
            throw e;
        }
    }

    /**
     * Handle floor button press request from REST controller.
     * Uses SCAN algorithm for optimal routing.
     */
    public void pressFloorButton(int targetFloorNumber) {
        log.info("REST request: Press floor button {}", targetFloorNumber);

        pressButtonCommand.setTargetFloor(targetFloorNumber);
        pressButtonCommand.executeCommand(elevatorState);

    }

    /**
     * Get all active floor requests
     */
    public ElevatorDTO getCurrentElevatorState() {
        return new ElevatorDTO(
                elevatorState.getCurrentFloor(),
                elevatorState.getCurrentMovementState(),
                elevatorState.getDirection(),
                elevatorState.getCurrentDoorState(),
                destinationManager.getAllDestinations());
    }

    //TODO: add elevator status DTO return

    // ==================== SCHEDULED PROCESSING (MAIN LOOP) ====================
    //run this function every second to simulate the elevator logic. we will check every
    //tick:
    //1. door status, are we open or closed?
    //2. can we move?
    //3. execute elevator movement
    @Scheduled(fixedRate = 1000)
    public void processElevatorOperations() {
        //wrapping in a try catch block for debugging and so the service main loop doesnt crash
        try {
            // 1. Check door operation
            handleDoorOperations();


            // 2. movement handling and validation
            handleElevatorMovement();
        } catch (Exception e) {
            log.error("Cannot process elevator operations: {}", e.getMessage());
        }

    }


    // ==================== Door Timing and validations ====================
    private void handleDoorOperations() {

        //if the elevator is moving we know the doors should be closed so we skip door logic loop
        if (elevatorState.getCurrentMovementState() == ElevatorMovement.MOVING) {
            return;
        }

        //grab the current timings since the last execution
        long currentTime = System.currentTimeMillis();
        long operationStartTime = elevatorState.getDoorOperationStartTimeMs();
        ElevatorDoor currentDoorState = elevatorState.getCurrentDoorState();


        switch (currentDoorState) {
            case OPENING:
                handleDoorOpening(currentTime, operationStartTime);
                break;

            case OPEN:
                handleDoorOpen(currentTime, operationStartTime);
                break;

            case CLOSING:
                handleDoorClosing(currentTime, operationStartTime);
                break;

            case CLOSED:
                // Nothing to do - doors are closed and stable
                log.trace("Doors are already closed at floor {}", elevatorState.getCurrentFloor());
                break;
        }
    }

    private void handleDoorOpening(long currentTime, long operationStartTime) {
        long elapsedTime = currentTime - operationStartTime;

        log.trace("Doors are still opening at floor {}", elevatorState.getCurrentFloor());

        //check if the doors transition from OPENING -> OPEN
        if (elapsedTime >= elevatorState.getDoorOperationTimeMs()) {
            // if true, we transition: OPENING -> OPEN
            elevatorState.setCurrentDoorState(ElevatorDoor.OPEN);
            elevatorState.setDoorOperationStartTimeMs(currentTime); // Reset timer for auto-close

            log.info("Doors fully opened at floor {}", elevatorState.getCurrentFloor());
        }
        //if not enough time has passed we wait for the next cycle
    }

    private void handleDoorOpen(long currentTime, long operationStartTime) {
        long elapsedTime = currentTime - operationStartTime;

        log.trace("Doors open at floor {}", elevatorState.getCurrentFloor());

        //check if enough time has elapsed to start auto close
        if (elapsedTime >= elevatorState.getDoorWaitTimeMs()) {
            // Auto-close time reached we transition: OPEN -> CLOSING
            log.info("Auto-closing doors at floor {} after {} ms", elevatorState.getCurrentFloor(), elapsedTime);

            // Use the close doors command to ensure safety validation
            closeDoorsCommand.executeCommand(elevatorState);
        }
        // If not time to auto-close yet, just wait for the next cycle
    }

    private void handleDoorClosing(long currentTime, long operationStartTime) {
        long elapsedTime = currentTime - operationStartTime;

        log.trace("Doors closing at floor {} ", elevatorState.getCurrentFloor());

        //check if enough time has passed to fully close the doors
        if (elapsedTime >= elevatorState.getDoorOperationTimeMs()) {
            // we Transition: CLOSING -> CLOSED
            elevatorState.setCurrentDoorState(ElevatorDoor.CLOSED);

            log.info("Doors fully closed at floor {}", elevatorState.getCurrentFloor());

            // log statement for when the elevator is idle with no new destinations
            if (!destinationManager.hasDestinations() &&
                    elevatorState.getCurrentMovementState() == ElevatorMovement.IDLE) {

                log.info("Elevator has no current destinations. State - Floor: {}, Movement: {}, Direction: {}",
                        elevatorState.getCurrentFloor(),
                        elevatorState.getCurrentMovementState(),
                        elevatorState.getDirection());
            }

            // Now elevator is ready for movement operations with the door closed
            // Clear the door operation timestamp since we're done
            elevatorState.setDoorOperationStartTimeMs(0);
        }
        // If not enough time has passed, just wait for next cycle
    }

    // ==================== ELEVATOR MOVEMENT LOGIC ====================

    private void handleElevatorMovement() {
        // we don't move if doors aren't closed, they will eventually close after some time
        if (elevatorState.getCurrentDoorState() != ElevatorDoor.CLOSED) {
            return;
        }

        ElevatorMovement currentMovement = elevatorState.getCurrentMovementState();

        //if doors are closed we handle the idle and moving states
        switch (currentMovement) {
            case IDLE:
                handleIdleState();
                break;
            case MOVING:
                handleMovingState();
                break;
        }
    }

    private void handleIdleState() {
        // Check if we have destinations
        if (!destinationManager.hasDestinations()) {
            log.trace("No destinations, remaining idle at floor {}", elevatorState.getCurrentFloor());
            return;
        }

        // Get next destination using SCAN algorithm
        Integer nextFloor = destinationManager.getNextDestination(elevatorState);

        if (nextFloor == null) {
            log.trace("No valid next destination");
            return;
        }

        // If we're already at the destination, handle the arrival immediately
        if (nextFloor.equals(elevatorState.getCurrentFloor())) {
            log.info("already at destination floor {} ", nextFloor);
            arriveAtTargetFloor();
            return;
        }

        // Start movement to next floor if we have any destinations in the queue
        startMovementToFloor(nextFloor);
    }

    private void handleMovingState() {
        long currentTime = System.currentTimeMillis();
        long movementStartTime = elevatorState.getMovementOperationStartTimeMs();

        if (movementStartTime <= 0) {
            log.error("Movement state is MOVING but no start time recorded!");
            return;
        }

        long elapsedTime = currentTime - movementStartTime;
        long travelTime = elevatorState.getFloorTravelTimeMs();

        // Check if enough time has passed to move one floor
        if (elapsedTime >= travelTime) {
            // Move one floor in the current direction
            moveOneFloor();
        } else {
            // Get current destination from destination manager for logging
            Integer currentDestination = destinationManager.getNextDestination(elevatorState);
            log.trace("Still moving to floor {} ({}ms remaining)",
                    currentDestination,
                    travelTime - elapsedTime);
        }
    }

    private void moveOneFloor() {
        ElevatorDirection direction = elevatorState.getDirection();
        int currentFloor = elevatorState.getCurrentFloor();
        int newFloor;

        // Move one floor in the current direction
        switch (direction) {
            case UP:
                newFloor = currentFloor + 1;
                break;
            case DOWN:
                newFloor = currentFloor - 1;
                break;
            default:
                log.error("Trying to move but direction is NONE!");
                return;
        }

        // Validate new floor is within bounds
        if (!elevatorState.isValidFloor(newFloor)) {
            log.error("Cannot move to invalid floor {}", newFloor);
            return;
        }

        // Update current floor
        elevatorState.setCurrentFloor(newFloor);
        log.info("Elevator Moving to floor {}", newFloor);

        // Check if we've reached ANY destination floor (NOT just the "next" one)
        List<Integer> allDestinations = destinationManager.getAllDestinations();
        if (allDestinations.contains(newFloor)) {
            // We've arrived at a destination floor
            log.info("Reached destination floor {}", newFloor);
            arriveAtTargetFloor();
        } else {
            // Reset timer for next floor movement
            elevatorState.setMovementOperationStartTimeMs(System.currentTimeMillis());
        }
    }
    private void startMovementToFloor(Integer nextRequestedFloor) {
        // Use ElevatorState for timing tracking only
        elevatorState.setMovementOperationStartTimeMs(System.currentTimeMillis());

        // Set direction based on target
        ElevatorDirection newMovementDirection = ElevatorDirection.between(
                elevatorState.getCurrentFloor(), nextRequestedFloor);

        //populate the state singleton with these values
        elevatorState.setDirection(newMovementDirection);
        elevatorState.setCurrentMovementState(ElevatorMovement.MOVING);

        log.info(" Started moving {} to floor {} (from floor {})",
                newMovementDirection.getDescription().toLowerCase(), nextRequestedFloor, elevatorState.getCurrentFloor());
    }

    private void arriveAtTargetFloor() {
        // We're already at the correct floor (updated by moveOneFloor)
        int currentFloor = elevatorState.getCurrentFloor();

        log.info("🏁 Arrived at destination floor {}", currentFloor);

        // Set elevator to idle
        elevatorState.setCurrentMovementState(ElevatorMovement.IDLE);

        try {
            // Remove this specific floor from destinations
            log.info("About to remove destination. Current destinations: {}", destinationManager.getAllDestinations());

            boolean removed = destinationManager.removeDestination(currentFloor);
            if (removed) {
                log.info("Successfully removed destination floor {}. Remaining destinations: {}",
                        currentFloor, destinationManager.getAllDestinations());
            } else {
                log.warn("Arrived at floor {} but it wasn't in the destination queue", currentFloor);
            }

            // Open doors automatically
            log.info("Opening doors at floor {}", currentFloor);
            openDoorsCommand.executeCommand(elevatorState);

            // Update direction for next destination
            updateDirectionForNextDestination();

        } catch (Exception e) {
            log.error("Error handling arrival at floor {}: {}", currentFloor, e.getMessage(), e);
        }

        // Clear movement timing
        elevatorState.setMovementOperationStartTimeMs(-1);

        log.info("Arrival complete. State - Floor: {}, Movement: {}, Direction: {}",
                elevatorState.getCurrentFloor(),
                elevatorState.getCurrentMovementState(),
                elevatorState.getDirection());
    }

    private void updateDirectionForNextDestination() {
        Integer nextFloor = destinationManager.getNextDestination(elevatorState);

        if (nextFloor != null) {
            ElevatorDirection nextDirection = ElevatorDirection.between(
                    elevatorState.getCurrentFloor(), nextFloor);
            elevatorState.setDirection(nextDirection);

            log.debug("Next destination: floor {}, direction: {}", nextFloor, nextDirection);
        } else {
            elevatorState.setDirection(ElevatorDirection.NONE);
            log.debug("No more destinations, direction set to NONE");
        }
    }
}
