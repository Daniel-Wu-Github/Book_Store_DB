FROM maven:3.8.8-eclipse-temurin-11 AS builder
WORKDIR /workspace

# Copy only the files required for a Maven build first to leverage Docker cache
COPY pom.xml mvnw* ./
COPY .mvn .mvn
RUN mkdir -p src

# Copy source and build
COPY src ./src
RUN mvn -B -DskipTests package -DskipTests

FROM eclipse-temurin:11-jre
WORKDIR /app
# Copy built jar from builder stage
COPY --from=builder /workspace/target/*.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
