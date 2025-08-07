package com.bluestaq.elevatorChallenge.service.commands;


import com.bluestaq.elevatorChallenge.service.ElevatorDoor;
import com.bluestaq.elevatorChallenge.service.ElevatorState;
import com.bluestaq.elevatorChallenge.service.SafetyValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A POJO that defines the open doors command on the elevator.
 * This will:
 * 1. Set doors to open state
 * 2. Transition elevator to DOORS_OPEN state
 * 3. Stop any movement (set direction to NONE)
 *
 */
 @Slf4j
 @Component
 public class OpenDoorsCommand implements ElevatorCommand {

    @Autowired
    SafetyValidator safetyValidator;

    @Override
    public boolean executeCommand(ElevatorState state) {

        // Quick exit if doors already open
        if (state.getCurrentDoorState() == ElevatorDoor.OPEN) {
            log.debug("Doors already open at floor {}", state.getCurrentFloor());
            return true;
        }

        //validate and execute the door Opening procedure
        if(canExecuteCommand(state)) {
            state.setCurrentDoorState(ElevatorDoor.OPENING);
            state.setDoorOperationStartTimeMs(System.currentTimeMillis());
            log.info("Door opening initiated at floor {}", state.getCurrentFloor());
            return true;
        }

        //if we get here we are in an error scenario

        throw new IllegalArgumentException("Cannot execute Open Doors command in current elevator state");

    }

    @Override
    public String getCommandType() {
        return "OPEN DOORS";
    }

    @Override
    public boolean canExecuteCommand(ElevatorState elevator) {

        return safetyValidator.canOpenDoors(elevator);
    }

}