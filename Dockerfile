FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY . .
RUN ./gradlew clean build

# Run stage
RUN cp build/libs/fireflyiii-agent-telegram.jar ./fireflyiii-agent-telegram.jar
ENTRYPOINT ["java", "-jar", "fireflyiii-agent-telegram.jar"]