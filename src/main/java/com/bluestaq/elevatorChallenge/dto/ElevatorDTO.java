package com.bluestaq.elevatorChallenge.dto;

import com.bluestaq.elevatorChallenge.service.ElevatorDirection;
import com.bluestaq.elevatorChallenge.service.ElevatorDoor;
import com.bluestaq.elevatorChallenge.service.ElevatorMovement;
import com.bluestaq.elevatorChallenge.service.ElevatorState;

import java.util.List;

public record ElevatorDTO(
        int currentFloor,
        ElevatorMovement state,
        ElevatorDirection direction,
        ElevatorDoor doorState,
        List<Integer> destinationFloors
) {}
