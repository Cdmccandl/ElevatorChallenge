package com.bluestaq.elevatorChallenge.service.commands;

import com.bluestaq.elevatorChallenge.service.ElevatorState;

public interface ElevatorCommand {

    boolean executeCommand(ElevatorState elevator);
    boolean canExecuteCommand(ElevatorState elevator);
    String getCommandType();
}
