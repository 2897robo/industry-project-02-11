#!/bin/bash

# 각 서비스에 Dockerfile이 없는 경우 생성하는 스크립트

echo "📝 Dockerfile 생성 중..."

# Eureka Service Dockerfile
cat > apps/eureka-discovery-service/Dockerfile << 'EOF'
FROM openjdk:21-jdk-slim
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
EXPOSE 8761
EOF

# Gateway Service Dockerfile
cat > apps/gateway-service/Dockerfile << 'EOF'
FROM openjdk:21-jdk-slim
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
EXPOSE 8080
EOF

# Auth Service Dockerfile
cat > apps/auth-service/Dockerfile << 'EOF'
FROM openjdk:21-jdk-slim
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
EOF

# User Service Dockerfile
cat > apps/user-service/Dockerfile << 'EOF'
FROM openjdk:21-jdk-slim
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
EOF

# Backend Service Dockerfile
cat > apps/backend/Dockerfile << 'EOF'
FROM openjdk:21-jdk-slim
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
EOF

echo "✅ Dockerfile 생성 완료!"
