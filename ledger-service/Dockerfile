# ---- Build ledger-service module ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY swiftpay-shared swiftpay-shared
COPY ledger-service ledger-service
COPY gateway-service/pom.xml gateway-service/pom.xml

RUN chmod +x mvnw && ./mvnw -B package -pl ledger-service -am -Dmaven.test.skip=true

# ---- Run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl
RUN addgroup -S swiftpay && adduser -S swiftpay -G swiftpay
USER swiftpay

COPY --from=build /app/ledger-service/target/ledger-service-*-exec.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
