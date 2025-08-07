package com.bluestaq.elevatorchallenge.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Singleton elevator component that manages the elevator's state and behavior.
 * This class represents a single elevator with its current position, direction, and destinations.
 * it is initialized at the ground floor ie floor 1
 * it will be in the idle state with the doors closed
 */

@Component
@Slf4j
@Getter
@Setter
public class ElevatorState {

    private final int id = 1;
    private int currentFloor = 1;
    private ElevatorDirection direction = ElevatorDirection.NONE;
    private ElevatorMovement currentMovementState = ElevatorMovement.IDLE;
    private ElevatorDoor currentDoorState = ElevatorDoor.CLOSED;

    //assuming no basement floors for this implementation
    private int minFloor = 1;

    //tracker for starting door operations
    private long doorOperationStartTimeMs = -1;

    //tracker for starting movement operations
    private long movementOperationStartTimeMs = -1;

    // Configuration injected from application.properties
    @Value("${elevator.max-floor:20}")
    private int maxFloor = 20;

    @Value("${elevator.floor-travel-time:4000}")
    private long floorTravelTimeMs = 1000;

    @Value("${elevator.door-operation-time:3000}")
    private long doorOperationTimeMs = 3000;

    @Value("${elevator.door-wait-time:5000}")
    private long doorWaitTimeMs = 5000;


    // ==================== VALIDATION METHODS ====================

    /**
     * Check if floor is within valid range
     */
    public boolean isValidFloor(int floor) {
        return floor >= minFloor && floor <= maxFloor;
    }

}
