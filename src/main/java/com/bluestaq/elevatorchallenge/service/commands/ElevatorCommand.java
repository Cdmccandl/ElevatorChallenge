package com.bluestaq.elevatorchallenge.service.commands;

import com.bluestaq.elevatorchallenge.service.ElevatorState;

//elevator command interface grouping similar functionality of a command
public interface ElevatorCommand {

    boolean executeCommand(ElevatorState elevator);
    boolean canExecuteCommand(ElevatorState elevator);
}
