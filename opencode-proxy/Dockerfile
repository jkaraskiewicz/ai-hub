# Multi-stage build for Kotlin/Native application
FROM gradle:8.14 AS builder

# Set working directory
WORKDIR /app

# Copy Gradle configuration files
COPY build.gradle.kts gradle.properties ./

# Copy source code
COPY src ./src

# Build the native application for the current architecture
RUN gradle clean linuxArm64Binaries --no-daemon

# Runtime stage - using minimal alpine image
FROM alpine:3.18

# Install necessary runtime dependencies
RUN apk add --no-cache gcompat

# Set working directory
WORKDIR /app

# Copy the built native executable from the builder stage
COPY --from=builder /app/build/bin/linuxArm64/releaseExecutable/kotlin-proxy.kexe app

# Make the executable runnable
RUN chmod +x app

# Expose the port the app runs on
EXPOSE 8080

# Run the application
CMD ["./app"]
