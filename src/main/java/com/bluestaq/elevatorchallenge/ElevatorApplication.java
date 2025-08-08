package com.bluestaq.elevatorchallenge;

import com.bluestaq.elevatorchallenge.service.ElevatorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class ElevatorApplication {

    @Autowired
    ElevatorState elevatorState;

	public static void main(String[] args) {
		SpringApplication.run(ElevatorApplication.class, args);
	}


    //call the onstartup method after the application context starts and publishes the applicationReadyEvent
    //want to log the elevator initialization here
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Elevator Ready - Initialized at floor {} with the Doors {}",
                elevatorState.getCurrentFloor(), elevatorState.getCurrentDoorState());
    }

}