# Stage 1: Build the Spring Boot application using a Maven or Gradle image
FROM maven:3.8.7-openjdk-17 AS build 
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests 

# Stage 2: Create the final lightweight runtime image
FROM openjdk:17-jre-slim
# A smaller JRE image for production
COPY --from=build /app/target/*.jar app.jar 

# Expose the port your Spring Boot app listens on (default is 8080)
EXPOSE 8089

# Command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "/app.jar"]
