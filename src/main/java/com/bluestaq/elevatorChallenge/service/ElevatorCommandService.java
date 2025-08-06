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

    // ==================== COMMAND QUEUING LOGIC ====================

    public void pressFloorButton(int floor) {
        commandQueue.enqueue(ElevatorCommand.pressButton(floor));
    }

    /**
     * Queue an open doors command
     */
    public void openDoors() {
        commandQueue.enqueue(ElevatorCommand.openDoors());
        log.info("Queued open doors command");
    }

    /**
     * Queue a close doors command
     */
    public void closeDoors() {
        commandQueue.enqueue(ElevatorCommand.closeDoors());
        log.info("Queued close doors command");
    }

    /**
     * Process commands from the commandQueue
     */
    private void processCommandQueue() {
        // Don't process commands if elevator is busy with specific timed operations
        if (elevator.isBusy()) {
            return;
        }

        // Don't process if queue is empty
        if (commandQueue.isEmpty()) {
            return;
        }

        // peek the next command
        ElevatorCommand command = commandQueue.peek();
        if (command == null) {
            commandQueue.dequeue();
            return;
        }

        // Check if command can be executed in current state
        if (command.canExecuteCommandInCurrentState(elevator)) {
            // Command executes its own logic and updates elevator state
            command.execute(elevator);
            log.info("Executed command: {}", command.getCommandType());
            commandQueue.dequeue();
        }
    }

    // ==================== SCHEDULED PROCESSING (MAIN LOOP) ====================

    //we need to regularly consume objects from the command queue in order to keep the elevator up to date

    @Scheduled(fixedRate = 1000)
    public void processElevatorOperations() {

        //Check the elevators movement and door timers
        checkMovementProgress();
        checkDoorOperationProgress();

        // 2. Process command queue if elevator is not busy with timed operations
        processCommandQueue();
    }


    // ==================== TIMING CHECKS ====================

    //we need to regularly check the elevators current state to determine when a command is complete

    /**
     * Check if the elevator is currently moving
     */
    private void checkMovementProgress() {
        if (elevator.isMovementInProgress()) {
            // Move one floor and report
            boolean movementComplete = elevator.moveToNextFloor();

            if (movementComplete) {
                // Movement to target complete, check what to do next
                handleMovementCompletion();
            }
        }
    }

    /**
     * Handle completion of movement to a floor
     */
    private void handleMovementCompletion() {
        if (elevator.shouldStopAtCurrentFloor()) {
            // We've reached a destination - open doors
            log.info("Reached destination floor {} - opening doors", elevator.getCurrentFloor());
            elevator.startDoorOperationTimer();
        } else if (elevator.hasDestinations()) {
            // More destinations - start movement to next one
            startMovementToNextDestination();
        } else {
            // No more destinations - go idle
            elevator.setCurrentState(ElevatorState.IDLE);
            log.info("All destinations reached - elevator idle at floor {}", elevator.getCurrentFloor());
        }
    }

    /**
     * Start movement to next destination in FIFO queue
     */
    private void startMovementToNextDestination() {
        if (!elevator.hasDestinations() || !elevator.canStartMovement()) {
            return;
        }

        Integer nextDestination = elevator.peekNextDestination();
        if (nextDestination != null) {
            elevator.startMovementToFloor(nextDestination);
        }
    }

    /**
     * Check door operation progress and complete if time elapsed
     */
    private void checkDoorOperationProgress() {
        if (!elevator.isDoorOperationInProgress()) {
            return;
        }

        long elapsedTime = elevator.getElapsedDoorOperationTime();

        if (elapsedTime >= elevator.getDoorOperationTimeMs()) {
            completeDoorOperation();
        }
    }

    /**
     * Complete door operation
     */
    private void completeDoorOperation() {
        elevator.stopDoorOperationTimer();

        if (elevator.getCurrentState() == ElevatorState.DOORS_OPEN && !elevator.isDoorsOpen()) {
            // Closing doors
            completeDoorClosing();
        } else {
            // Opening doors
            completeDoorOpening();
        }
    }

    private void completeDoorOpening() {
        elevator.setDoorsOpen(true);
        elevator.setCurrentState(ElevatorState.DOORS_OPEN);
        log.info("Doors opened at floor {}", elevator.getCurrentFloor());
    }

    private void completeDoorClosing() {
        elevator.setDoorsOpen(false);
        log.info("Doors closed at floor {}", elevator.getCurrentFloor());

        if (elevator.hasDestinations()) {
            startMovementToNextDestination();
        } else {
            elevator.setCurrentState(ElevatorState.IDLE);
        }
    }

}
