# Elevator Challenge
A homework coding challenge given to me by Bluestaq to simulate an Elevator. There was an additional request to containerize this service as well

[![Build Status](https://app.travis-ci.com/Cdmccandl/ElevatorChallenge.svg?token=QA1pjUWHEzzRjMXv5zS3&branch=main)](https://app.travis-ci.com/Cdmccandl/ElevatorChallenge) [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Cdmccandl_ElevatorChallenge&metric=bugs)](https://sonarcloud.io/summary/new_code?id=Cdmccandl_ElevatorChallenge)[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Cdmccandl_ElevatorChallenge&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Cdmccandl_ElevatorChallenge)

[![SonarQube Cloud](https://sonarcloud.io/images/project_badges/sonarcloud-light.svg)](https://sonarcloud.io/summary/new_code?id=Cdmccandl_ElevatorChallenge)


## Assumptions
Some of the assumptions that I made for this implementation
- There is only one elevator.
- The amount of people walking into or exiting the elevator is untracked.
- The weight limit of the elevator is untracked, it will always function
- There are no basement floors

## Features
- Configurable number of floors at startup.
- Configurable time to move between floors at startup.
- Configurable time to wait while elevator door is open on a floor at startup.
- Configurable time to wait for the doors to open or close
- Can press the open door button to reset the open door timer.
- An OpenAPI/Swagger interface visualizing api Docs for the application
- Can request to close an elevator door early, before the open action is complete
- Can press any number of floor buttons in any order at any given time in the elevator via REST (including OpenAPI/Swagger UI).
- The elevator algorithm, when it is running, will continually move upward until it reaches the top level requested. Then toggle back in the downward direction if necessary for
 for additional requests (Utilizes SCAN algorithm)
- Sample test classes that include some JUnit/Mockito unit tests to display how unit testing would be done on this type of application. As well as a SpringBootTest
  to confirm that the application starts.
- Usage of global REST exception handling that significantly reduces code duplication and allows for meaningful response bodies from REST calls
- Travis CI integration to show build results, codeQL integration to show security concerns on the codebase.

## Features Not Implemented
- Change the number of floors or elveator timers during RUNTIME
- A spring logging framework, with this being asked to include a containerized option this would have been a nice touch
- More than one elevator, I believe the design pattern applied here allows for scalability when adding more elevators
- Only minimal javadoc was actually put in place, generally on classes themselves but I commented lots throughout to detail the specific algorithm/design patterns applied
- Some form of front end implementation, even some ASCII graphics returned from the rest request would be interesting.

## Development Tools
This project was built with the following tools & framework:
- Java 21
- Spring Boot 3.4.3
- Maven 3.9.11
- Podman 5.5.2
- Intellij IDEA 2025.2

## How To Build, Run & Test
The below steps assumes you have already configured your machine with the above development tools. 

1. ```git clone``` the project to a directory of your choice.
2. Navigate to the root directory of the project
3. ```mvn clean package```
4. ```java -jar target/ElevatorChallenge-1.0.0-SNAPSHOT.jar``` - this will use the default configuration properties if you change the version elected in the pom you will
   see that updated here
6. Observe the console startup success noting the amount of elevator floors and the starting floor in the application console log
7. Now that we have confirmed the compiled jar starts up and is functioning, lets return to the terminal and build the container
8. ```podman build -t elevator-challenge .```
9. ```podman images``` make sure you see the newly created image localhost/elevator-challenge
10. deploy and run the container locally ```podman run -p 8080:8080 elevator-challenge```
11. Open your browser to **http://localhost:8080/ElevatorChallenge/swagger-ui/index.html#/**
12. Review the endpoints and test them as you wish, check the logs outputted in the container to verify. You can also verify the response bodies of many of the endpoints through swagger as well

Additionally, the following configuration properties can be set on application startup
the defaults that I have chosen are as follows:
- elevator.max-floor (default - 20)
- elevator.floor-travel-time (default - 1000ms)
- elevator.door-operation-time(default - 3000ms)
- elevator.door-wait-time(default - 5000ms)

Here is an example service launch command to override the simulation to have 40 floors:<br/><br/>
```java -jar target/ElevatorChallenge-1.0.0-SNAPSHOT.jar --elevator.max-floor=40```
You can also launch the container via podman with these overrides
```podman run -p 8080:8080 elevator-challenge --elevator.max-floor=40```
