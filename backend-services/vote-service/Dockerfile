# Stage 1: Build the application using Maven with Eclipse Temurin JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the parent POM and sub-module POMs
COPY pom.xml .
COPY identity-service/pom.xml identity-service/
COPY election-service/pom.xml election-service/
COPY vote-service/pom.xml vote-service/
COPY audit-service/pom.xml audit-service/

# Cache Maven dependencies for vote-service
RUN mvn dependency:go-offline -B -pl vote-service -am

# Copy vote-service source and build package
COPY vote-service/src vote-service/src
RUN mvn package -DskipTests -pl vote-service -am

# Stage 2: Minimal runtime image using Eclipse Temurin JRE 17
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/vote-service/target/*.jar app.jar
EXPOSE 8083
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
