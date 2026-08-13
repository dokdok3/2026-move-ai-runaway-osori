FROM eclipse-temurin:25-jre

RUN useradd --system --uid 10001 app
WORKDIR /app
COPY build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
