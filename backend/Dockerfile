FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
RUN useradd --create-home --uid 10001 spring
WORKDIR /app
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R spring:spring /app/uploads
USER spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
	CMD ["java", "-version"]
ENTRYPOINT ["java","-jar","/app/app.jar"]
