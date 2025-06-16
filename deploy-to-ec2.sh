#!/bin/bash

# EC2 서버 정보
EC2_HOST="ubuntu@ip-172-31-36-62"
PROJECT_PATH="~/industry-project-02-11/11조/6프로그램"

echo "🚀 EC2 서버에 Spring Boot 서비스 배포 시작..."

# SSH 명령으로 EC2에서 직접 실행
ssh -o StrictHostKeyChecking=no $EC2_HOST << 'ENDSSH'
cd ~/industry-project-02-11/11조/6프로그램

echo "📦 기존 컨테이너 정리..."
sudo docker-compose down 2>/dev/null || true
sudo docker system prune -f

echo "📝 docker-compose.local.yml 파일 생성..."
cat > docker-compose.local.yml << 'EOF'
version: '3.8'

services:
  postgres:
    image: postgres:13
    container_name: team11-postgres
    environment:
      POSTGRES_DB: cloudcost
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:6.2-alpine
    container_name: team11-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  eureka-service:
    build: ./eureka-service
    image: team11-eureka
    container_name: team11-eureka
    ports:
      - "8761:8761"
    environment:
      SPRING_PROFILES_ACTIVE: docker
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
    depends_on:
      - postgres
      - redis

  gateway-service:
    build: ./gateway-service
    image: team11-gateway
    container_name: team11-gateway
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-service:8761/eureka/
    depends_on:
      - eureka-service

  auth-service:
    build: ./auth-service
    image: team11-auth
    container_name: team11-auth
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-service:8761/eureka/
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/cloudcost
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    depends_on:
      - eureka-service
      - postgres
      - redis

  user-service:
    build: ./user-service
    image: team11-user
    container_name: team11-user
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-service:8761/eureka/
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/cloudcost
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
    depends_on:
      - eureka-service
      - postgres

  backend-service:
    build: ./backend-service
    image: team11-backend
    container_name: team11-backend
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-service:8761/eureka/
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/cloudcost
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    depends_on:
      - eureka-service
      - postgres
      - redis

volumes:
  postgres_data:
    driver: local

networks:
  default:
    name: team11-network
EOF

echo "🔧 각 서비스에 Dockerfile 생성..."

# Eureka Service Dockerfile
mkdir -p eureka-service
cat > eureka-service/Dockerfile << 'EOF'
FROM openjdk:21-jdk-slim
VOLUME /tmp
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EOF

# Gateway Service Dockerfile
mkdir -p gateway-service
cat > gateway-service/Dockerfile << 'EOF'
FROM op