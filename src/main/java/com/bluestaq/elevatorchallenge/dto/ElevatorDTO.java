package com.bluestaq.elevatorchallenge.dto;

import com.bluestaq.elevatorchallenge.service.ElevatorDirection;
import com.bluestaq.elevatorchallenge.service.ElevatorDoor;
import com.bluestaq.elevatorchallenge.service.ElevatorMovement;

import java.util.List;
import java.util.Set;

public record ElevatorDTO(
        int currentFloor,
        ElevatorMovement state,
        ElevatorDirection direction,
        ElevatorDoor doorState,
        List<Integer> destinationFloors,
        Set<Integer> upwardDestinations,
        Set<Integer> downwardDestinations
) {}
