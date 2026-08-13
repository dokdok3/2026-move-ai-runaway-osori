FROM eclipse-temurin:25-jre AS extractor

WORKDIR /builder
COPY build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre

RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=extractor /builder/extracted/dependencies/ ./
COPY --from=extractor /builder/extracted/spring-boot-loader/ ./
COPY --from=extractor /builder/extracted/snapshot-dependencies/ ./
COPY --from=extractor /builder/extracted/application/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
