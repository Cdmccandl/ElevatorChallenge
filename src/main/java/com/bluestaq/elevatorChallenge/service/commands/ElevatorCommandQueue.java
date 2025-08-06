package com.bluestaq.elevatorChallenge.service.commands;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * This class is used to track the queued commands that the elevator recieves
 * it will store ElevatorCommands in a FIFO algormithm approach
 *
 * @author Conor McCandless
 */

@Component
@Slf4j
public class ElevatorCommandQueue {

    //for simplicity sake I am electing to use FIFO here instead of a priority queue algorithm.
    // the java concurrent library has a data structure that supports this in a threaded manner
    private final ConcurrentLinkedQueue<ElevatorCommand> commandQueue = new ConcurrentLinkedQueue<>();

    //since I am using the ConcurrentLinkedQueue I dont have to explicitly define these as synchronized
    public void enqueue(ElevatorCommand command) {
        commandQueue.offer(command);
        log.info("Command queued: {}", command.getCommandType());
    }

    public ElevatorCommand dequeue() {
        return commandQueue.poll(); // Returns null if empty
    }

    public ElevatorCommand peek() {
        return commandQueue.peek(); // Returns null if empty
    }

    public boolean isEmpty() {
        return commandQueue.isEmpty();
    }

    public int size() {
        return commandQueue.size();
    }

    //I do have to synchronize this as it involves multiple operations
    public synchronized List<String> getCurrentQueue() {
        return commandQueue.stream()
                .map(ElevatorCommand::getCommandType)
                .collect(Collectors.toList());
    }

    public void clear() {
        commandQueue.clear();
        log.info("Command queue cleared");
    }
}
