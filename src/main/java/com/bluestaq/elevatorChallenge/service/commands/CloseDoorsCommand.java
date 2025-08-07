package com.bluestaq.elevatorChallenge.service.commands;


import com.bluestaq.elevatorChallenge.service.ElevatorDoor;
import com.bluestaq.elevatorChallenge.service.ElevatorState;
import com.bluestaq.elevatorChallenge.service.SafetyValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CloseDoorsCommand implements ElevatorCommand {

    @Autowired
    SafetyValidator safetyValidator;


    @Override
    public boolean executeCommand(ElevatorState state) {

        // Quick exit if doors already closed
        if (state.getCurrentDoorState() == ElevatorDoor.CLOSED) {
            log.info("Doors already closed at floor {}", state.getCurrentFloor());
            return true;
        }

        //Validate and then execute the door closing procedure
        if(canExecuteCommand(state)) {
            state.setCurrentDoorState(ElevatorDoor.CLOSING);
            state.setDoorOperationStartTimeMs(System.currentTimeMillis());
            log.info("Door closing initiated at floor {}", state.getCurrentFloor());
            return true;
        }

        // If we get here we are in an error scenario, let this exception bubble up to the restControllerAdvice
        //for a descriptive HTTP response
        throw new IllegalArgumentException("Cannot execute Close Doors command in current elevator state");
    }

    @Override
    public boolean canExecuteCommand(ElevatorState elevator) {
        return safetyValidator.canCloseDoors(elevator);
    }

}
