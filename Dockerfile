# Use a base Linux image with JDK 21 (Debian-based), can be replaced with an IRONBANK image if desired
FROM eclipse-temurin:21-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR file into the container
COPY target/ElevatorChallenge-*.jar app.jar

# Expose port for web traffic
EXPOSE 8080

# Command to run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
