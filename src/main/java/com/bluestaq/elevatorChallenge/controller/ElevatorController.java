package com.bluestaq.elevatorChallenge.controller;

import com.bluestaq.elevatorChallenge.service.ElevatorCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("")
@Tag(name = "Elevator Command Requests", description = "Command Requests available for controlling the elevator.")
public class ElevatorController {

    @Autowired
    ElevatorCommandService elevatorService;

    // Open door request
    @Operation(summary = "Press Open Door button in Elevator",
            description = "Press Open Door button in Elevator")
    @GetMapping("/pressOpenDoor")
    public void requestOpenDoor() {
        elevatorService.openDoors();
    }

    // Close door request
    @Operation(summary = "Press Close Door button in Elevator",
            description = "Press Close Door button in Elevator")
    @GetMapping("/pressCloseDoor")
    public void requestCloseDoor() {
        elevatorService.closeDoors();
    }

    // Press floor number request
    @Operation(summary = "Press any floor button in Elevator",
            description = "Press any floor button in Elevator")
    @GetMapping("/pressFloorNumber")
    public void requestFloorNumber(@RequestParam int targetFloorNumber) {
        elevatorService.pressFloorButton(targetFloorNumber);
    }

//    // Get all the floor requests active
//    @Operation(summary = "Queries Elevator for all active requests it is tracking",
//            description = "Queries Elevator for all active requests it is tracking")
//    @GetMapping("/activeFloorRequests")
//    public List<FloorDto> getActiveFloorRequests() {
//        return elevatorService.getActiveFloorRequests();
//    }
}
