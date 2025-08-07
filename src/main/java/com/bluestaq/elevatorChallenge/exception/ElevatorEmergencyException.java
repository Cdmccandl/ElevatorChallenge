package com.bluestaq.elevatorChallenge.exception;

public class ElevatorEmergencyException extends RuntimeException {

    public ElevatorEmergencyException() {
        super("Elevator is in emergency stop mode - all operations are blocked");
    }

    public ElevatorEmergencyException(String message) {
        super(message);
    }
}