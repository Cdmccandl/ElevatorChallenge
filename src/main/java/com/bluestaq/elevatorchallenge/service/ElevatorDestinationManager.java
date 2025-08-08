package com.bluestaq.elevatorchallenge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Thread-safe destination manager that implements intelligent elevator scheduling.
 * Uses a classic SCAN elevator algorithm to minimize travel time by serving floors in
 * directional order rather than FIFO like a traditional queue
 *
 * here is the steps I decided to use for my Algorithm
 * 1. Continue in current direction until no more floors in that direction
 * 2. Reverse direction and serve floors in the opposite direction
 * 3. Optimize by grouping floors by direction from current position
 *
 * this works in an asynchronous environment like a spring-web microservice as it can serve floor requests while in movement
 * and intelligently place them so the elevator doesnt serve floor requests in a silly unoptimized order
 *
 */
@Component
@Slf4j
public class ElevatorDestinationManager {

    // Thread-safe sorted sets - no explicit mutex lock needed.
    private final ConcurrentSkipListSet<Integer> upwardFloors = new ConcurrentSkipListSet<>();
    private final ConcurrentSkipListSet<Integer> downwardFloors = new ConcurrentSkipListSet<>(Collections.reverseOrder());

    /**
     * Add a destination floor intelligently based on elevator state
     */
    public boolean addDestination(int targetFloor, ElevatorState elevatorState) {
        int currentFloor = elevatorState.getCurrentFloor();
        ElevatorDirection currentDirection = elevatorState.getDirection();


        // Don't add if already at target floor
        if (targetFloor == currentFloor) {
            log.debug("Target floor {} is current floor, not adding to destinations", targetFloor);
            return false;
        }

        // Validate floor is in valid range
        if (!elevatorState.isValidFloor(targetFloor)) {
            log.warn("Invalid floor {}: must be between {} and {}",
                    targetFloor, elevatorState.getMinFloor(), elevatorState.getMaxFloor());
            return false;
        }

        // check if floor already exists for cleaner logging
        boolean alreadyExists = upwardFloors.contains(targetFloor) || downwardFloors.contains(targetFloor);

        if (alreadyExists) {
            log.info("Floor {} already requested, ignoring duplicate request", targetFloor);
            return false;  // Don't add duplicate
        }

        // Remove from both sets if present to avoid duplicates, this isnt necessary but it is safe to have
        // could be removed if coverage is needed
        upwardFloors.remove(targetFloor);
        downwardFloors.remove(targetFloor);

        boolean shouldGoUp = shouldAddToUpwardSet(targetFloor, currentFloor, currentDirection);
        // Determine which set to add to based on current position and direction
        if (shouldGoUp) {
            upwardFloors.add(targetFloor);
        } else {
            downwardFloors.add(targetFloor);
        }

        log.info("Added floor {} to destinations. current Destination List: {}",
                targetFloor, getAllDestinations());
        return true;
    }

    /**
     * Get the next destination based on current elevator state.
     * Implements the SCAN algorithm: continue in current direction until no more floors,
     * then reverse direction.
     */
    public Integer getNextDestination(ElevatorState elevatorState) {
        int currentFloor = elevatorState.getCurrentFloor();
        ElevatorDirection currentDirection = elevatorState.getDirection();

        // If no destinations, return null
        if (upwardFloors.isEmpty() && downwardFloors.isEmpty()) {
            return null;
        }

        Integer nextFloor = null;

        // Determine next floor based on current direction using SCAN algorithm
        switch (currentDirection) {
            case UP:
                // Going up: find the next floor above current, or switch direction
                nextFloor = getNextFloorInDirection(currentFloor, ElevatorDirection.UP);
                if (nextFloor == null) {
                    // No floors above, switch to downward direction
                    nextFloor = getNextFloorInDirection(currentFloor, ElevatorDirection.DOWN);
                }
                break;

            case DOWN:
                // Going down: find the next floor below current, or switch direction
                nextFloor = getNextFloorInDirection(currentFloor, ElevatorDirection.DOWN);
                if (nextFloor == null) {
                    // No floors below, switch to upward direction
                    nextFloor = getNextFloorInDirection(currentFloor, ElevatorDirection.UP);
                }
                break;

            case NONE: // When stationary, start with upward preference
                // First try upward direction
                nextFloor = getNextFloorInDirection(currentFloor, ElevatorDirection.UP);
                if (nextFloor == null) {
                    // No floors above, try downward
                    nextFloor = getNextFloorInDirection(currentFloor, ElevatorDirection.DOWN);
                }
                break;
        }

        if (nextFloor != null) {
            log.trace("Next destination: {} (current: {}, direction: {})",
                    nextFloor, currentFloor, currentDirection);
        }

        return nextFloor;
    }



    /**
     * Remove a destination from the specified list. This should be called on floor arrival
     */
    public boolean removeDestination(int floor) {
        boolean removedFromUp = upwardFloors.remove(floor);
        boolean removedFromDown = downwardFloors.remove(floor);

        boolean wasRemoved = removedFromUp || removedFromDown;

        if (wasRemoved) {
            log.info("Removed floor {} from destinations. Up: {}, Down: {}",
                    floor, upwardFloors, downwardFloors);
        } else {
            log.warn("Floor {} was not found in either destination set", floor);
        }

        return wasRemoved;
    }

    /**
     * Check if there are any remaining destinations
     */
    public boolean hasDestinations() {
        return !upwardFloors.isEmpty() || !downwardFloors.isEmpty();
    }

    /**
     * Get all destinations for display/logging purposes
     */
    public List<Integer> getAllDestinations() {
        List<Integer> allDestinations = new ArrayList<>();
        allDestinations.addAll(upwardFloors);
        allDestinations.addAll(downwardFloors);
        Collections.sort(allDestinations);
        return allDestinations;
    }

    /**
     * Clear all destinations (emergency stop)
     */
    public void clearAllDestinations() {
        upwardFloors.clear();
        downwardFloors.clear();
        log.info("All destinations cleared");
    }

    /**
     * Get count of destinations
     */
    public int getDestinationCount() {
        return upwardFloors.size() + downwardFloors.size();
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Get the next floor in the specified direction from current floor.
     * Implements nearest-first depending on the passed in direction.
     *
     */
    private Integer getNextFloorInDirection(int currentFloor, ElevatorDirection currentDirection) {
        Set<Integer> floorsInDirection = null;
        if (currentDirection == ElevatorDirection.UP) {
            floorsInDirection = upwardFloors;
        } else {
            floorsInDirection = downwardFloors;
        }
        if (floorsInDirection.isEmpty()) {
            return null;
        }

        if (currentDirection == ElevatorDirection.UP) {
            // Going up: find the NEXT floor above current (closest first)
            Integer nextUp = null;
            for (Integer floor : floorsInDirection) {
                if (floor > currentFloor && (nextUp == null || floor < nextUp)) {
                    nextUp = floor;
                }
            }
            // If no floors above current, get any floor in the upward set
            if (nextUp == null && !floorsInDirection.isEmpty()) {
                nextUp = Collections.min(floorsInDirection);
            }
            return nextUp;
        } else {
            // Going down: find the NEXT floor below current (closest first)
            Integer nextDown = null;
            for (Integer floor : floorsInDirection) {
                if (floor < currentFloor && (nextDown == null || floor > nextDown)) {
                    nextDown = floor;
                }
            }

            //this is an edge case that we likely dont need but is safe to have
            if (nextDown == null && !floorsInDirection.isEmpty()) {
                nextDown = Collections.max(floorsInDirection);
            }
            return nextDown;
        }
    }

    /**
     * Determine if a floor should be added to the upward set based on elevator state.
     * Uses the ElevatorDirection enum for cleaner logic.
     */
    private boolean shouldAddToUpwardSet(int targetFloor, int currentFloor, ElevatorDirection currentDirection) {
        boolean targetIsAbove = targetFloor > currentFloor;

        switch (currentDirection) {
            case UP:
                // If going up, floors above go to upward set, floors below go to downward set
                // This ensures we finish the current direction before switching
                return targetIsAbove;

            case DOWN:
                // If going down, floors below go to downward set, floors above go to upward set
                return !targetIsAbove;

            case NONE:
            default:
                // When stationary, prefer upward direction for floors above, downward for floors below
                return targetIsAbove;
        }
    }
}
