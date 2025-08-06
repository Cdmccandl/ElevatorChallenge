package com.bluestaq.elevatorChallenge.service.commands;

import com.bluestaq.elevatorChallenge.service.Elevator;

public interface ElevatorCommand {

    void execute(Elevator elevator);

    String getCommandType();

    boolean canExecuteCommandInCurrentState(Elevator elevator);


    //these static factory methods are used in this interface instead of an abstract class to define
    //the commands at the interface level
    static ElevatorCommand openDoors() {
        return new OpenDoorsCommand();
    }

    static ElevatorCommand closeDoors() {
        return new CloseDoorsCommand();
    }

    static ElevatorCommand pressButton(int floor) {
        return new PressButtonCommand(floor);
    }

//    static ElevatorCommand emergencyStop() {
//        return new EmergencyStopCommand();
//    }
}
