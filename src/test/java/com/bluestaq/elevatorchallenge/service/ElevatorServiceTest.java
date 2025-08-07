package com.bluestaq.elevatorchallenge.service;

import com.bluestaq.elevatorchallenge.service.commands.CloseDoorsCommand;
import com.bluestaq.elevatorchallenge.service.commands.OpenDoorsCommand;
import com.bluestaq.elevatorchallenge.service.commands.PressButtonCommand;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ElevatorServiceTest {

    @Spy
    ElevatorState elevator;

    @Spy
    ElevatorDestinationManager destinationManager;

    @Mock
    SafetyValidator safetyValidator;

    @InjectMocks
    private OpenDoorsCommand openDoorsCommand = Mockito.spy(OpenDoorsCommand.class);

    @InjectMocks
    private CloseDoorsCommand closeDoorsCommand = Mockito.spy(CloseDoorsCommand.class);

    @InjectMocks
    private PressButtonCommand pressButtonCommand = Mockito.spy(PressButtonCommand.class);

    @InjectMocks
    ElevatorService elevatorCommandService;

    @Test
    public void testPressOpenDoorCommandWhenClosed() {

        Mockito.doReturn(true).when(safetyValidator).canOpenDoors(Mockito.any());
        elevatorCommandService.openDoors();

        //run a loop of the main elevator loop
        elevatorCommandService.processElevatorOperations();

        assertEquals(ElevatorDoor.OPENING, elevatorCommandService.getCurrentElevatorState().doorState());

    }

    @Test
    public void testPressCloseDoorWhenClosedDoesNotThrowException() {


        elevatorCommandService.closeDoors();

        assertDoesNotThrow(() -> {
            elevatorCommandService.processElevatorOperations();
        });
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
