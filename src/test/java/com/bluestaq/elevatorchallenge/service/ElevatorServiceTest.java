package com.bluestaq.elevatorchallenge.service;

import com.bluestaq.elevatorchallenge.exception.ElevatorEmergencyException;
import com.bluestaq.elevatorchallenge.service.commands.CloseDoorsCommand;
import com.bluestaq.elevatorchallenge.service.commands.OpenDoorsCommand;
import com.bluestaq.elevatorchallenge.service.commands.PressButtonCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

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
    ElevatorService elevatorService;


    @BeforeEach
    void setUp() {

        // Reset elevator to default state for each test
        elevator.setCurrentFloor(1);
        elevator.setDirection(ElevatorDirection.NONE);
        elevator.setCurrentMovementState(ElevatorMovement.IDLE);
        elevator.setCurrentDoorState(ElevatorDoor.CLOSED);
        elevator.setDoorOperationStartTimeMs(-1);
        elevator.setMovementOperationStartTimeMs(-1);

        // Clear destinations
        destinationManager.clearAllDestinations();
    }

    @Test
    public void testPressOpenDoorCommandWhenClosed() {

        Mockito.doReturn(true).when(safetyValidator).canOpenDoors(Mockito.any());
        elevatorService.openDoors();
        //run a loop of the main elevator loop
        elevatorService.processElevatorOperations();
        assertEquals(ElevatorDoor.OPENING, elevatorService.getCurrentElevatorState().doorState());
    }

    @Test
    public void testPressCloseDoorWhenClosedDoesNotThrowException() {
        elevatorService.closeDoors();
        assertDoesNotThrow(() -> {
            elevatorService.processElevatorOperations();
        });
    }

    @Test
    void testOpenDoorsWhenEmergencyModeThrowsException() {
        // setup
        elevator.setCurrentMovementState(ElevatorMovement.EMERGENCY);
        assertThrows(ElevatorEmergencyException.class, () -> elevatorService.openDoors());
    }

    @Test
    void testCloseDoorsWhenEmergencyModeThrowsException() {
        // setup
        elevator.setCurrentMovementState(ElevatorMovement.EMERGENCY);
        assertThrows(ElevatorEmergencyException.class, () -> elevatorService.closeDoors());
    }

    @Test
    @Disabled
    public void testPressFloorButtonWhileIdleState() {


        //consume it from the command queue
        elevatorService.pressFloorButton(5);
        elevatorService.processElevatorOperations();

        // add some floor requests to the queue

        elevatorService.processElevatorOperations();


    }

    @Test
    void testPressFloorButtonWhenCurrentFloorAndDoorsClosedAndIdle() {
        // setup
        elevator.setCurrentFloor(5);
        elevator.setCurrentDoorState(ElevatorDoor.CLOSED);
        elevator.setCurrentMovementState(ElevatorMovement.IDLE);

        Mockito.doReturn(true).when(safetyValidator).canOpenDoors(Mockito.any());
        //check no exception
        assertDoesNotThrow(() -> elevatorService.pressFloorButton(5));

        // check doors are opened and no destination is added
        Mockito.verify(openDoorsCommand, Mockito.times(1)).executeCommand(elevator);
        Mockito.verify(pressButtonCommand, Mockito.times(0)).executeCommand(elevator);
    }



}
