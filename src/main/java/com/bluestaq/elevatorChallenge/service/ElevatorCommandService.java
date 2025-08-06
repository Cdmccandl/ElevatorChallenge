package com.bluestaq.elevatorChallenge.service;


import com.bluestaq.elevatorChallenge.service.commands.ElevatorCommand;
import com.bluestaq.elevatorChallenge.service.commands.ElevatorCommandQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ElevatorCommandService {

    @Autowired
    private ElevatorCommandQueue commandQueue;

    @Autowired
    private Elevator elevator;

    // Track automatic door cycle
    private boolean isAutomaticDoorCycle = false;
    private long doorWaitStartTimeMs = 0;
    private boolean isWaitingForPassengers = false;

    // ==================== COMMAND QUEUING LOGIC ====================

    public void pressFloorButton(int floor) {
        if (!elevator.isValidFloor(floor)) {
            throw new IllegalArgumentException("Invalid floor " + floor + "must be between " + elevator.getMinFloor() + "and " + elevator.getMaxFloor());
            }
            commandQueue.enqueue(ElevatorCommand.pressButton(floor));

    }

    /**
     * Queue an open doors command, check to see if elevator is in motion
     */
    public void openDoors() {

        if(elevator.isMoving()) {
            throw new IllegalArgumentException("Cannot open doors: elevator is moving between floors");
        }
        commandQueue.enqueue(ElevatorCommand.openDoors());
        log.info("Queued open doors command");
    }

    /**
     * Queue a close doors command
     */
    public void closeDoors() {

        if(elevator.isMoving()) {
            throw new IllegalArgumentException("Doors are already closed and elevator is moving between floors");
        }

        commandQueue.enqueue(ElevatorCommand.closeDoors());
        log.info("Queued close doors command");
    }

    // ==================== SCHEDULED PROCESSING (MAIN LOOP) ====================
    @Scheduled(fixedRate = 1000)
    public void processElevatorOperations() {
        // 1. Check movement progress
        checkMovementProgress();

        // 2. Check door operation progress
        checkDoorOperationProgress();

        // 3. Check passenger wait time
        checkPassengerWaitTime();

        // 4. Process command queue if not busy
        processCommandQueue();
    }

    // ==================== FIXED COMMAND PROCESSING ====================

    private void processCommandQueue() {
        // Don't process if elevator is busy or waiting for passengers
        if (elevator.isBusy() || isWaitingForPassengers) {
            return;
        }

        if (commandQueue.isEmpty()) {
            return;
        }

        ElevatorCommand command = commandQueue.peek();
        if (command == null) {
            return;
        }

        // Check if command can execute
        if (command.canExecuteCommandInCurrentState(elevator)) {
            // Remove from queue
            commandQueue.dequeue();

            // Track if this is a manual door operation
            String commandType = command.getCommandType();
            if (commandType.equals("OPEN DOORS") || commandType.equals("CLOSE DOORS")) {
                isAutomaticDoorCycle = false;  // Mark as manual operation
            }

            // Execute the command
            command.execute(elevator);
            log.info("Executed command: {}", commandType);

        } else {
            // Command cannot execute in current state
            log.debug("Command {} cannot execute in state {}, leaving in queue",
                    command.getCommandType(), elevator.getCurrentState());
        }
    }

    // ==================== MOVEMENT HANDLING ====================

    private void checkMovementProgress() {
        if (!elevator.isMovementInProgress()) {
            return;
        }

        boolean movementComplete = elevator.moveToNextFloor();

        if (movementComplete) {
            handleMovementCompletion();
        }
    }

    private void handleMovementCompletion() {
        // Check if we stopped at a destination
        if (elevator.getCurrentState() == ElevatorState.IDLE && !elevator.isMovementInProgress()) {
            log.info("Reached destination, starting automatic door cycle");
            startAutomaticDoorCycle();
        }
    }

    private void startAutomaticDoorCycle() {
        isAutomaticDoorCycle = true;

        if (elevator.startDoorOpening()) {
            log.info("Started automatic door opening at floor {}", elevator.getCurrentFloor());
        } else {
            log.warn("Failed to start automatic door opening");
            isAutomaticDoorCycle = false;
        }
    }

    // ==================== DOOR OPERATION HANDLING ====================

    private void checkDoorOperationProgress() {
        if (!elevator.isDoorOperationInProgress()) {
            return;
        }

        long elapsedTime = elevator.getElapsedDoorOperationTime();

        if (elapsedTime >= elevator.getDoorOperationTimeMs()) {
            // Door operation complete
            Elevator.DoorOperationType operationType = elevator.getCurrentDoorOperation();

            // Complete the operation
            elevator.completeDoorOperation();

            // Handle post-operation logic
            if (operationType == Elevator.DoorOperationType.OPENING) {
                if (isAutomaticDoorCycle) {
                    // Start passenger wait for automatic cycle
                    startPassengerWait();
                }
                // For manual open, doors just stay open until manually closed
            } else if (operationType == Elevator.DoorOperationType.CLOSING) {
                if (isAutomaticDoorCycle) {
                    // Automatic close complete
                    isAutomaticDoorCycle = false;
                }
                // Check for next destination
                checkForNextDestination();
            }
        }
    }

    private void startPassengerWait() {
        isWaitingForPassengers = true;
        doorWaitStartTimeMs = System.currentTimeMillis();
        log.info("Waiting {}ms for passengers", elevator.getDoorWaitTimeMs());
    }

    private void checkPassengerWaitTime() {
        if (!isWaitingForPassengers) {
            return;
        }

        long elapsedWaitTime = System.currentTimeMillis() - doorWaitStartTimeMs;

        if (elapsedWaitTime >= elevator.getDoorWaitTimeMs()) {
            // Wait complete, close doors
            isWaitingForPassengers = false;

            if (elevator.startDoorClosing()) {
                log.info("Passenger wait complete, closing doors automatically");
            }
        }
    }

    private void checkForNextDestination() {
        if (elevator.hasDestinations()) {
            startMovementToNextDestination();
        } else {
            log.info("No more destinations, elevator idle at floor {}",
                    elevator.getCurrentFloor());
        }
    }

    // ==================== MOVEMENT INITIATION ====================

    private void startMovementToNextDestination() {
        if (!elevator.hasDestinations() || !elevator.canStartMovement()) {
            return;
        }

        Integer nextDestination = elevator.peekNextDestination();
        if (nextDestination != null) {
            if (nextDestination.equals(elevator.getCurrentFloor())) {
                // Already at this floor
                elevator.pollNextDestination();
                log.info("Already at destination floor {}", nextDestination);
                startAutomaticDoorCycle();
            } else {
                elevator.startMovementToFloor(nextDestination);
            }
        }
    }
}
