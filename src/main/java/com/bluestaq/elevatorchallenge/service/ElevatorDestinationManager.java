package com.bluestaq.elevatorchallenge.service;

import lombok.Getter;
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
 * 4. Serve floors in a nearest first manner as they are added to each set
 *
 * this works in an asynchronous environment like a spring-web microservice as it can serve floor requests while in movement
 * and intelligently place them so the elevator doesnt serve floor requests in a silly unoptimized order
 *
 */
@Component
@Slf4j
public class ElevatorDestinationManager {

    // Thread-safe sorted sets - no explicit mutex lock needed.
    @Getter
    final ConcurrentSkipListSet<Integer> upwardFloors = new ConcurrentSkipListSet<>();
    @Getter
    final ConcurrentSkipListSet<Integer> downwardFloors = new ConcurrentSkipListSet<>(Collections.reverseOrder());

    /**
     * Add a destination floor intelligently based on elevator state
     */
    public boolean addDestination(int targetFloor, ElevatorState elevatorState) {
        int currentFloor = elevatorState.getCurrentFloor();

        // check if floor already exists for cleaner logging
        boolean alreadyExists = upwardFloors.contains(targetFloor) || downwardFloors.contains(targetFloor);

        if (alreadyExists) {
            log.info("Floor {} already requested, ignoring duplicate request", targetFloor);
            return false;  // Don't add duplicate
        }

        // Remove from both sets if present to avoid duplicates
        upwardFloors.remove(targetFloor);
        downwardFloors.remove(targetFloor);

        boolean shouldGoUp = shouldAddToUpwardSet(targetFloor, currentFloor);
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
     * Add floor calling request with explicit direction (for UP/DOWN buttons on floors)
     * This bypasses the SCAN direction logic and directly places floors in the requested queue
     */
    public boolean addFloorRequestWithDirection(int targetFloor, ElevatorDirection requestedDirection, ElevatorState elevatorState) {

        // Check for duplicates
        boolean alreadyExists = upwardFloors.contains(targetFloor) || downwardFloors.contains(targetFloor);
        if (alreadyExists) {
            log.info("Floor {} already requested with direction: {}, ignoring duplicate request", targetFloor, requestedDirection);
            return false;
        }

        // Remove from both sets (safety)
        upwardFloors.remove(targetFloor);
        downwardFloors.remove(targetFloor);

        // Add to correct queue based on requested direction
        if (requestedDirection == ElevatorDirection.UP) {
            upwardFloors.add(targetFloor);
        } else {
            downwardFloors.add(targetFloor);
        }

        log.info("Added floor {} to destinations (direction {}). Current destinations: UP: {}  DOWN: {}",
                targetFloor, requestedDirection, upwardFloors,  downwardFloors);
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

        // SCAN algorithm implementation
        switch (currentDirection) {
            case UP:
                // Going up, we look for floors above current in upward set
                nextFloor = getNextFloorAboveCurrent(currentFloor, upwardFloors);
                if (nextFloor == null) {
                    // No more floors above in upward direction, switch to downward
                    nextFloor = getNextFloorBelowCurrent(currentFloor, downwardFloors);
                    // If still no floor below current, get highest floor in downward set
                    if (nextFloor == null && !downwardFloors.isEmpty()) {
                        nextFloor = Collections.max(downwardFloors);
                    }
                }
                break;

            case DOWN:
                // Going down we look for floors below current in downward set
                nextFloor = getNextFloorBelowCurrent(currentFloor, downwardFloors);
                if (nextFloor == null) {
                    // No more floors below in downward direction, switch to upward
                    nextFloor = getNextFloorAboveCurrent(currentFloor, upwardFloors);
                    // If still no floor above current, get lowest floor in upward set
                    if (nextFloor == null && !upwardFloors.isEmpty()) {
                        nextFloor = Collections.min(upwardFloors);
                    }
                }
                break;

            case NONE: // When stationary, start with upward preference
                // First try upward direction
                nextFloor = getNextFloorAboveCurrent(currentFloor, upwardFloors);
                if (nextFloor == null && !upwardFloors.isEmpty()) {
                    nextFloor = Collections.min(upwardFloors);
                }
                if (nextFloor == null) {
                    // No floors in upward set, try downward
                    nextFloor = getNextFloorBelowCurrent(currentFloor, downwardFloors);
                    if (nextFloor == null && !downwardFloors.isEmpty()) {
                        nextFloor = Collections.max(downwardFloors);
                    }
                }
                break;
        }

        if (nextFloor != null) {
            log.trace("Next destination: {} (current: {}, direction: {}, upward floors: {}, downward floors: {})",
                    nextFloor, currentFloor, currentDirection, upwardFloors, downwardFloors);
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
     * Finds the closest floor above the current floor from the specified set.
     * This method implements the nearest-first strategy
     */
    private Integer getNextFloorAboveCurrent(int currentFloor, Set<Integer> floorSet) {
        Integer nextUp = null;
        for (Integer floor : floorSet) {
            if (floor > currentFloor && (nextUp == null || floor < nextUp)) {
                nextUp = floor;
            }
        }
        return nextUp;
    }

    /**
     * Finds the closest floor below the current floor from the specified set.
     * This method implements the nearest-first strategy
     */
    private Integer getNextFloorBelowCurrent(int currentFloor, Set<Integer> floorSet) {
        Integer nextDown = null;
        for (Integer floor : floorSet) {
            if (floor < currentFloor && (nextDown == null || floor > nextDown)) {
                nextDown = floor;
            }
        }
        return nextDown;
    }

    private boolean shouldAddToUpwardSet(int targetFloor, int currentFloor) {
        //floors above current position go to upward set, floors below go to downward set
        return targetFloor > currentFloor;
    }
}
