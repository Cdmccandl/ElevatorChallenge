package com.bluestaq.elevatorchallenge.service;

import com.bluestaq.elevatorchallenge.exception.ElevatorEmergencyException;
import com.bluestaq.elevatorchallenge.service.commands.CloseDoorsCommand;
import com.bluestaq.elevatorchallenge.service.commands.OpenDoorsCommand;
import com.bluestaq.elevatorchallenge.service.commands.PressButtonCommand;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for the service layer of this microservice. This is where most of the business logic resides
 * For the sake of time I wrote mostly basic functionality testing and did not cover all edge cases. Using
 * this as an example of how I format test names and strategies
 */
@ExtendWith(MockitoExtension.class)
public class ElevatorServiceTest {

    @Spy
    ElevatorState elevator;
    @Spy
    ElevatorDestinationManager destinationManager;
    @Mock
    SafetyValidator safetyValidator;
    //have to do this type of spy instantiation in order to inject relevant dependencies into dependent classes for
    //class under test
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

    // =================== Door State Tests ================================
    @Test
    public void testPressOpenDoorCommandWhenClosed() {
        Mockito.doReturn(true).when(safetyValidator).canOpenDoors(Mockito.any());
        elevatorService.openDoors();
        //run a loop of the main elevator loop
        elevatorService.processElevatorOperations();
        assertEquals(ElevatorDoor.OPENING, elevatorService.getCurrentElevatorState().doorState());
    }

    @Test
    public void testPressOpenDoorCommandWhenMoving() {
        Mockito.doReturn(false).when(safetyValidator).canOpenDoors(Mockito.any());
        elevator.setCurrentMovementState(ElevatorMovement.MOVING);
        assertThrows(IllegalArgumentException.class, () -> elevatorService.openDoors());
    }

    @Test
    public void testPressCloseDoorWhenClosedDoesNotThrowException() {
        elevatorService.closeDoors();
        assertDoesNotThrow(() -> {
            elevatorService.processElevatorOperations();
        });
    }

    // =================== Floor Button Tests ================================
    @Test
    public void testPressFloorButtonOnceWhileIdleState() {

        elevatorService.pressFloorButton(5);
        elevatorService.processElevatorOperations();
        // check the floor queue is populated
        assertFalse(elevatorService.destinationManager.getAllDestinations().isEmpty());
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

    @Test
    public void testElevatorMovesWhenFloorButtonPressed() {

        elevatorService.pressFloorButton(5);
        elevatorService.processElevatorOperations();
        // check the floor queue is populated
        assertSame(ElevatorDirection.UP, elevator.getDirection());
    }

    @Test
    public void testElevatorClearsFloorRequestWhenTargetFloorReached() {

        elevatorService.pressFloorButton(5);
        elevatorService.processElevatorOperations();
        // check the floor queue is populated
        assertFalse(elevatorService.destinationManager.getAllDestinations().isEmpty());
        //set current floor to 5 for test, we are skipping simulating the working loop
        elevator.setCurrentFloor(5);
        elevator.setCurrentMovementState(ElevatorMovement.IDLE);
        elevatorService.processElevatorOperations();
        //check that our destination queue is empty
        assertTrue(elevatorService.destinationManager.getAllDestinations().isEmpty());
    }

    @Test
    public void testElevatorDoesNotAddDuplicateFloorRequests() {
        elevatorService.pressFloorButton(5);
        elevatorService.pressFloorButton(5);
        elevatorService.processElevatorOperations();
        // check the floor queue is populated with ONE element
        assertEquals(1, elevatorService.destinationManager.getAllDestinations().size());
    }

    // =================== Emergency Button Tests ================================
    @Test
    void testEmergencyStopClearsAllDestinationsAndSetsState() {
        elevatorService.pressFloorButton(5);
        elevatorService.processElevatorOperations();
        elevatorService.pressFloorButton(7);
        elevatorService.processElevatorOperations();
        assertEquals(2, elevatorService.destinationManager.getAllDestinations().size());

       //press emergency button
        elevatorService.emergencyStop();
        assertTrue(elevatorService.destinationManager.getAllDestinations().isEmpty());
        assertEquals(ElevatorMovement.EMERGENCY,  elevator.getCurrentMovementState());
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
    void testPressButtonWhenEmergencyModeThrowsException() {
        // setup
        elevator.setCurrentMovementState(ElevatorMovement.EMERGENCY);
        assertThrows(ElevatorEmergencyException.class, () -> elevatorService.pressFloorButton(1));
    }

    @Test
    void testClearEmergencyRemovesEmergencyState() {
        //press emergency button
        elevatorService.emergencyStop();
        //assert that an open doors command throws exception
        assertThrows(ElevatorEmergencyException.class, () -> elevatorService.openDoors());
        //clear the emergency
        elevatorService.emergencyClear();
        //exception is NOT thrown
        assertDoesNotThrow(() -> elevatorService.pressFloorButton(5));
    }
}
