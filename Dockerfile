# Step 1: Build the app using Maven
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run the app using a lightweight JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expose the port (Render sets PORT env var dynamically)
EXPOSE 8080
ENV PORT 8080

CMD ["sh", "-c", "java -jar app.jar --server.port=$PORT"]