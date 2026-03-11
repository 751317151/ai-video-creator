FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY avc-common/pom.xml avc-common/pom.xml
COPY avc-ai/pom.xml avc-ai/pom.xml
COPY avc-infra/pom.xml avc-infra/pom.xml
COPY avc-web/pom.xml avc-web/pom.xml
COPY avc-scheduler/pom.xml avc-scheduler/pom.xml
COPY avc-app/pom.xml avc-app/pom.xml

RUN mvn dependency:go-offline -B

COPY . .

RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /app/avc-app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
