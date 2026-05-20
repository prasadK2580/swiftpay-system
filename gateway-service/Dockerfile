# ---- Build gateway-service module ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY swiftpay-shared swiftpay-shared
COPY gateway-service gateway-service
COPY ledger-service/pom.xml ledger-service/pom.xml

RUN chmod +x mvnw && ./mvnw -B package -pl gateway-service -am -Dmaven.test.skip=true

# ---- Run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl
RUN addgroup -S swiftpay && adduser -S swiftpay -G swiftpay
USER swiftpay

COPY --from=build /app/gateway-service/target/gateway-service-*-exec.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
