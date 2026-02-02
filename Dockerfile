FROM maven:3.8.8-eclipse-temurin-11 AS builder
WORKDIR /workspace

# Copy the full project into the builder and build. We purposely avoid
# copying non-existent paths like `.mvn` or `mvnw*` to prevent build errors
# on platforms where those files aren't present.
COPY . ./
RUN mvn -B -DskipTests package

FROM eclipse-temurin:11-jre
WORKDIR /app
# Copy built jar from builder stage
COPY --from=builder /workspace/target/*.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
