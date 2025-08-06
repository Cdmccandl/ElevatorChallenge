package com.bluestaq.elevatorChallenge.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Singleton elevator component that manages the elevator's state and behavior.
 * This class represents a single elevator with its current position, state, and destinations.
 * it is initialized at the ground floor ie floor 1
 * it will be in the idle state with the doors closed
 */

@Component
@Slf4j
@Getter
@Setter
public class Elevator {

    private final int id = 1;
    private int currentFloor = 1;
    private ElevatorDirection direction = ElevatorDirection.NONE;
    private ElevatorState currentState = ElevatorState.IDLE;
    private boolean doorsOpen = false;
    //to keep things simple I am going to use FIFO, so I decided its best
    //to use a Linked List to accomplish this
    private final Queue<Integer> destinationFloors = new LinkedList<>();

    //Movement Tracking variables
    private boolean isMovementInProgress = false;
    private int movementTargetFloor = 1; // Current movement target

    // Door operation timing
    private long doorOperationStartTimeMs = 0;
    private boolean isDoorOperationInProgress = false;

    //assuming no basement floors for this implementation
    private int minFloor = 1;

    // Configuration injected from application.properties
    @Value("${elevator.max-floor:20}")
    private int maxFloor;

    @Value("${elevator.floor-travel-time:2000}")
    private long floorTravelTimeMs;

    @Value("${elevator.door-operation-time:2000}")
    private long doorOperationTimeMs;


    // ==================== MOVEMENT STATE MANAGEMENT ====================

    /**
     * Start movement towards a specific floor
     */
    public void startMovementToFloor(int targetFloor) {
        if (!isValidFloor(targetFloor)) {
            log.warn("Cannot start movement to invalid floor: {}", targetFloor);
            return;
        }
        if (targetFloor == currentFloor) {
            log.info("Already at target floor {}", targetFloor);
            return;
        }

        //if above checks pass start floor movement and set the direction
        isMovementInProgress = true;
        movementTargetFloor = targetFloor;
        direction = ElevatorDirection.between(currentFloor, targetFloor);
        setCurrentState(ElevatorState.MOVING);

        log.info("Started movement from floor {} to floor {} (direction: {})",
                currentFloor, targetFloor, direction);
    }

    /**
     * Move to the next floor in the current direction
     */
    public boolean moveToNextFloor() {
        if (!isMovementInProgress) {
            log.warn("Cannot move: no movement in progress");
            return false;
        }
        if (direction == ElevatorDirection.NONE) {
            log.warn("Cannot move: no direction set");
            return false;
        }

        //direction enum has a positive value for up direction and negative for down
        int nextFloor = currentFloor + direction.getValue();

        //make sure we arent moving to a floor above the floor count
        if (!isValidFloor(nextFloor)) {
            log.warn("Cannot move to floor {} it does not exist", nextFloor);
            stopMovement();
            return false;
        }

        // Move to next floor
        currentFloor = nextFloor;
        log.info("moved to floor {}", currentFloor);

        // Check if we've reached our target
        if (currentFloor == movementTargetFloor) {
            completeMovementToTarget();
            return true; // Movement complete, we process the next available command
        }

        return false; // Movement continues
    }

    /**
     * Complete movement to target floor
     */
    private void completeMovementToTarget() {
        //set movement state variables to indicate finished
        isMovementInProgress = false;
        movementTargetFloor = -1;
        direction = ElevatorDirection.NONE;

        // Check if this floor is a destination
        if (shouldStopAtCurrentFloor()) {
            Integer reachedDestination = pollNextDestination();
            log.info("Reached destination floor {}", reachedDestination);
            // Start door opening will be handled by service
        } else {
            // Not a destination, check for next destination
            if (hasDestinations()) {
                // More destinations to process
                log.debug("More destinations remaining: {}", getDestinationList());
            } else {
                // No more destinations, go idle
                setCurrentState(ElevatorState.IDLE);
                log.info("No more destinations - elevator going idle");
            }
        }
    }

    /**
     * Stop movement immediately
     */
    public void stopMovement() {
        isMovementInProgress = false;
        movementTargetFloor = -1;
        direction = ElevatorDirection.NONE;
        setCurrentState(ElevatorState.IDLE);
        log.info("Movement stopped at floor {}", currentFloor);
    }

    // ==================== DOOR OPERATION TIMING ====================

    /**
     * Start the door operation timer
     */
    public void startDoorOperationTimer() {
        isDoorOperationInProgress = true;
        doorOperationStartTimeMs = System.currentTimeMillis();
        log.debug("Door operation timer started");
    }

    /**
     * Stop the door operation timer
     */
    public void stopDoorOperationTimer() {
        isDoorOperationInProgress = false;
        doorOperationStartTimeMs = 0;
        log.debug("Door operation timer stopped");
    }

    /**
     * Get elapsed time since door operation started
     */
    public long getElapsedDoorOperationTime() {
        if (!isDoorOperationInProgress) {
            return 0;
        }
        return System.currentTimeMillis() - doorOperationStartTimeMs;
    }

    // ==================== BUSY FLAG LOGIC ====================

    /**
     * Check if elevator is busy with any operation
     * BUSY = movement in progress OR door operation in progress
     */
    public boolean isBusy() {
        return isMovementInProgress || isDoorOperationInProgress;
    }


    // ==================== DESTINATION QUEUE OPERATIONS ====================

    /**
     * Add a destination to the FIFO queue
     */
    public void addDestination(int floor) {
        destinationFloors.offer(floor);
        log.debug("Added floor {} to queue. Current queue: {}", floor, getDestinationList());
    }

    /**
     * Get the next destination without removing it
     */
    public Integer peekNextDestination() {
        return destinationFloors.peek();
    }

    /**
     * Remove and return the next destination from the queue
     */
    public Integer pollNextDestination() {
        Integer removed = destinationFloors.poll();
        if (removed != null) {
            log.debug("Removed floor {} from queue. Remaining: {}", removed, getDestinationList());
        }
        return removed;
    }

    /**
     * Check if should stop at current floor
     */
    public boolean shouldStopAtCurrentFloor() {
        Integer nextDestination = peekNextDestination();
        return nextDestination != null && nextDestination.equals(currentFloor);
    }

    /**
     * Check if has pending destinations
     */
    public boolean hasDestinations() {
        return !destinationFloors.isEmpty();
    }

    /**
     * Clear all destinations
     */
    public void clearDestinations() {
        destinationFloors.clear();
        log.info("Cleared all destinations");
    }

    /**
     * Get destinations as an ordered list
     */
    public List<Integer> getDestinationList() {
        return new ArrayList<>(destinationFloors);
    }

    /**
     * Get destinations as a set (for DTO compatibility)
     */
    public Set<Integer> getDestinationFloors() {
        return new LinkedHashSet<>(destinationFloors);
    }

    // ==================== VALIDATION METHODS ====================

    /**
     * Check if floor is within valid range
     */
    public boolean isValidFloor(int floor) {
        return floor >= minFloor && floor <= maxFloor;
    }

    /**
     * Check if elevator is currently moving
     */
    public boolean isMoving() {
        return currentState == ElevatorState.MOVING;
    }

    /**
     * Check if elevator is ready to start new movement
     */
    public boolean canStartMovement() {
        return !isMovementInProgress && !isDoorOperationInProgress;
    }




}
