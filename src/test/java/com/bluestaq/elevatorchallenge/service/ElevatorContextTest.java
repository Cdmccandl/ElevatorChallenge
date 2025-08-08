package com.bluestaq.elevatorchallenge.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * SpringBootTest that spins up and starts the service, I could do some controller layer testing here but for
 * the sake of time I am using this to ensure the service starts without failure at the unit testing stage
 */
@SpringBootTest
public class ElevatorContextTest {


    @Test
    public void contextLoads() {
        //boilerplate to ensure the spring application context instantiates and
        //the service starts

    }
}
