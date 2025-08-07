package com.bluestaq.elevatorChallenge.service.commands;

import com.bluestaq.elevatorChallenge.service.ElevatorState;

//elevator command interface grouping similar functionality of a command
public interface ElevatorCommand {

    boolean executeCommand(ElevatorState elevator);
    boolean canExecuteCommand(ElevatorState elevator);
}
