FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY furutabi/.mvn .mvn
COPY furutabi/mvnw furutabi/pom.xml ./
RUN chmod +x ./mvnw

COPY furutabi/src src
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", "-Dserver.port=10000", "-jar", "/app/app.jar"]
