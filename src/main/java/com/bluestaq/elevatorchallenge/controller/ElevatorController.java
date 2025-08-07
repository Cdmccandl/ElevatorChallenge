package com.bluestaq.elevatorchallenge.controller;

import com.bluestaq.elevatorchallenge.dto.ElevatorDTO;
import com.bluestaq.elevatorchallenge.service.ElevatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("")
@Tag(name = "Elevator Command Requests", description = "Command Requests available for controlling the elevator.")
public class ElevatorController {

    @Autowired
    ElevatorService elevatorService;

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
            description = "Press any floor button in Elevator. Uses SCAN algorithm for optimal routing.")
    @GetMapping("/pressFloorNumber")
    public void requestFloorNumber(@RequestParam int targetFloorNumber) {
        elevatorService.pressFloorButton(targetFloorNumber);
    }

    // Get the current status of the elevator
    @Operation(summary = "Queries Elevator for its current Status",
            description = "Queries Elevator for all active requests it is tracking as well as its direction,door, and movement information")
    @GetMapping("/currentElevatorState")
    public ElevatorDTO getCurrentElevatorState() {
        return elevatorService.getCurrentElevatorState();
    }

    @Operation(summary = "Emergency Stop",
            description = "Immediately stops elevator and blocks all operations")
    @PostMapping("/emergency/stop")
    public void emergencyStop() {
        elevatorService.emergencyStop();
    }

    @Operation(summary = "Clear Emergency Stop",
            description = "Restores normal elevator operation")
    @PostMapping("/emergency/clear")
    public void emergencyClear() {
        elevatorService.emergencyClear();
    }
}
