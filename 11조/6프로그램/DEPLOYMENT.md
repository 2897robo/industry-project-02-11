# 배포 가이드

## 사전 준비사항

1. Docker 및 Docker Compose 설치
2. Java 17 이상
3. Node.js 18 이상

## 로컬 배포

### 1. 전체 서비스 실행

```bash
# 데이터베이스 먼저 실행
docker-compose up -d

# 각 서비스 빌드
cd apps/eureka-discovery-service && ./gradlew build && cd ../..
cd apps/gateway-service && ./gradlew build && cd ../..
cd apps/auth-service && ./gradlew build && cd ../..
cd apps/user-service && ./gradlew build && cd ../..
cd apps/backend && ./gradlew build && cd ../..

# 전체 서비스 실행
docker-compose -f docker-compose.yml -f docker-compose.services.yml up -d
```

### 2. 서비스 확인

- Eureka Dashboard: http://localhost:8761
- API Gateway: http://localhost:8000
- Frontend: http://localhost:5173

## 프로덕션 배포

### AWS EC2 배포

1. EC2 인스턴스 생성 (t3.large 이상 권장)
2. 보안 그룹 설정:
   - 8000 (API Gateway)
   - 80/443 (Frontend)
   - 22 (SSH)

3. 서버 설정:
```bash
# Docker 설치
sudo yum update -y
sudo yum install docker -y
sudo systemctl start docker
sudo usermod -a -G docker ec2-user

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 프로젝트 클론
git clone [repository-url]
cd [project-directory]
```

4. 환경 변수 설정:
```bash
# .env 파일 생성
cat > .env << EOF
SPRING_PROFILES_ACTIVE=prod
DB_HOST=your-rds-endpoint
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password
AES_KEY=your-32-char-aes-key
JWT_SECRET=your-jwt-secret
EOF
```

5. 배포:
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### K-PaaS 배포

1. CF CLI 설치
2. 각 서비스별 manifest.yml 작성
3. 서비스 배포:
```bash
# 각 서비스 디렉토리에서
./gradlew build
cf push
```

## 모니터링

### 헬스 체크
- Eureka: http://localhost:8761/actuator/health
- Gateway: http://localhost:8000/actuator/health
- Backend: http://localhost:8000/resource-service/actuator/health

### 로그 확인
```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f backend-service
```

## 트러블슈팅

### 서비스가 Eureka에 등록되지 않는 경우
1. Eureka 서비스가 정상 실행 중인지 확인
2. 네트워크 설정 확인
3. 서비스 로그에서 연결 오류 확인

### 데이터베이스 연결 실패
1. PostgreSQL 컨테이너 상태 확인
2. 연결 정보 (호스트, 포트, 인증정보) 확인
3. Flyway 마이그레이션 로그 확인

### 프론트엔드 API 호출 실패
1. Gateway 서비스 상태 확인
2. CORS 설정 확인
3. 프록시 설정 확인
