#!/bin/bash

# EC2 긴급 배포 스크립트
# 이 스크립트는 EC2 서버에서 직접 실행합니다.

set -e

echo "🚀 Spring Boot 마이크로서비스 긴급 배포 시작..."

# 프로젝트 디렉토리로 이동
cd ~/industry-project-02-11/11조/6프로그램

# 기존 컨테이너 정리
echo "📦 기존 컨테이너 정리..."
sudo docker-compose down 2>/dev/null || true
sudo docker system prune -f

# 각 서비스 빌드 및 이미지 생성
echo "🔨 각 서비스 빌드 중..."

services=("eureka-discovery-service" "gateway-service" "auth-service" "user-service" "backend")

for service in "${services[@]}"; do
    echo "  📌 $service 빌드 중..."
    cd apps/$service
    
    # Gradle 빌드
    chmod +x gradlew
    ./gradlew clean bootJar
    
    # Docker 이미지 빌드
    sudo docker build -t team11-${service} .
    
    cd ../..
done

# docker-compose.yml 파일 생성
echo "📝 docker-compose.yml 파일 생성..."
cat > docker-compose.yml << 'EOF'
version: "3.8"

services:
  postgres:
    image: postgres:13
    container_name: team11-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: team11
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
    image: team11-eureka-discovery-service
    container_name: team11-eureka
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

  gateway-service:
    image: team11-gateway-service
    container_name: team11-gateway
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-service:8761/eureka/
    depends_on:
      - eureka-service

  auth-service:
    image: team11-auth-service
    container_name: team11-auth
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-service:8761/eureka/
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/team11
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
    depends_on:
      - eureka-service
      - postgres
      - redis

  user-service:
    image: team11-user-service
    container_name: team11-user
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-service:8761/eureka/
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/team11
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
    depends_on:
      - eureka-service
      - postgres

  backend-service:
    image: team11-backend
    container_name: team11-backend
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-service:8761/eureka/
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/team11
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
    depends_on:
      - eureka-service
      - postgres
      - redis

  nginx:
    image: nginx:alpine
    container_name: team11-nginx
    ports:
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - gateway-service

volumes:
  postgres_data:
    driver: local

networks:
  default:
    name: team11-network
EOF

# nginx 설정 파일 생성
echo "📝 nginx 설정 파일 생성..."
mkdir -p nginx
cat > nginx/nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    upstream gateway {
        server gateway-service:8080;
    }

    server {
        listen 80;
        
        location / {
            proxy_pass http://gateway;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
EOF

# 서비스 시작
echo "🚀 서비스 시작..."
sudo docker-compose up -d

# 상태 확인
echo "📊 서비스 상태 확인..."
sleep 10
sudo docker-compose ps

echo "✅ 배포 완료!"
echo "🌐 다음 주소로 접속 가능합니다:"
echo "   - Eureka: http://$(curl -s ifconfig.me):8761"
echo "   - Gateway: http://$(curl -s ifconfig.me):8080"
echo "   - Application: http://$(curl -s ifconfig.me)"

# 로그 확인 명령어 안내
echo ""
echo "📋 로그 확인 명령어:"
echo "   sudo docker-compose logs -f [서비스명]"
echo "   예: sudo docker-compose logs -f eureka-service"
