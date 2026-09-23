# Use a lightweight Java 17 runtime image
FROM eclipse-temurin:17-jre-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the compiled application and configuration file
COPY out/production/API_gateway /app
COPY gateway.properties /app/gateway.properties

# Expose port 8080 for incoming traffic
EXPOSE 8080

# Start the API Gateway
CMD ["java", "ApiGateway"]