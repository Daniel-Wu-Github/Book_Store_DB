FROM eclipse-temurin:11-jre
WORKDIR /app
# Default Dockerfile (backend). This exists so platforms that expect a
# top-level `Dockerfile` (Render default) can build the backend image.
# The actual backend jar must be present at target/*.jar after `mvn package`.
COPY target/*.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
