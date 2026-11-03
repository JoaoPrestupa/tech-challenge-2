# Estágio de build: compila o jar, sem levar o Maven para a imagem final.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q -DskipTests package

# Estágio de execução: somente JRE, com usuário sem privilégios.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 --no-create-home library
WORKDIR /app
COPY --from=build /workspace/target/library-api.jar app.jar
USER library
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
