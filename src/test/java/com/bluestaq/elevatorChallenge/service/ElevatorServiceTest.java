package com.bluestaq.elevatorChallenge.service;

import com.bluestaq.elevatorChallenge.service.commands.PressButtonCommand;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ElevatorServiceTest {

    @Mock
    ElevatorState elevator;

    @Spy
    ElevatorDestinationManager destinationManager;

    @InjectMocks
    PressButtonCommand pressButton;

    @InjectMocks
    ElevatorService elevatorCommandService;

    @Test
    @Disabled
    public void testPressOpenDoorCommandOnIdleState() {

        //create the openDoors command via the factory
        elevatorCommandService.openDoors();

        //consume it from the command queue
        elevatorCommandService.processElevatorOperations();

        elevatorCommandService.processElevatorOperations();


    }

    @Test
    @Disabled
    public void testPressCloseDoorCommandOnIdleState() {

        //create the openDoors command via the factory
        elevatorCommandService.closeDoors();

        //consume it from the command queue
        elevatorCommandService.processElevatorOperations();

        elevatorCommandService.processElevatorOperations();


    }

    @Test
    @Disabled
    public void testPressFloorButtonWhileIdleState() {


        //consume it from the command queue
        elevatorCommandService.pressFloorButton(5);
        elevatorCommandService.processElevatorOperations();

        // add some floor requests to the queue

        elevatorCommandService.processElevatorOperations();


    }



}
