# AWS EC2 Ubuntu 배포 가이드

## 1. EC2 인스턴스 준비

### 1.1 EC2 인스턴스 생성
- **AMI**: Ubuntu Server 22.04 LTS
- **인스턴스 타입**: t3.large (최소 권장)
- **스토리지**: 30GB 이상
- **보안 그룹 설정**:
  ```
  - SSH (22) - 관리자 IP만
  - HTTP (80) - 0.0.0.0/0
  - HTTPS (443) - 0.0.0.0/0
  - Eureka (8761) - 관리자 IP만
  - Gateway (8000) - 0.0.0.0/0
  ```

### 1.2 EC2 접속
```bash
ssh -i "your-key.pem" ubuntu@your-ec2-public-ip
```

## 2. 시스템 설정

### 2.1 시스템 업데이트
```bash
sudo apt update && sudo apt upgrade -y
```

### 2.2 필수 패키지 설치
```bash
# 기본 도구 설치
sudo apt install -y curl wget git vim htop

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.3/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER
newgrp docker
```

### 2.3 AWS CLI 설치 및 설정
```bash
# AWS CLI v2 설치
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt install -y unzip
unzip awscliv2.zip
sudo ./aws/install

# AWS 인증 설정 (IAM 역할 사용 권장)
aws configure
```

## 3. ECR 로그인 설정

```bash
# ECR 로그인 (리전은 실제 사용 리전으로 변경)
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com
```

## 4. 프로젝트 배포

### 4.1 프로젝트 클론
```bash
cd ~
git clone https://github.com/your-repo/industry-project-02-11.git
cd industry-project-02-11/11조/6프로그램
```

### 4.2 환경변수 설정
```bash
# .env 파일 생성
cp .env.prod.example .env
vim .env
```

`.env` 파일 내용 (실제 값으로 변경):
```env
# Database Configuration (AWS RDS)
DB_URL=jdbc:postgresql://your-rds-endpoint.ap-northeast-2.rds.amazonaws.com:5432/team11_cloud_cost
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# Redis Configuration (AWS ElastiCache)
REDIS_HOST=your-redis-cluster.cache.amazonaws.com
REDIS_PORT=6379

# Security Keys
JWT_SECRET=your-very-long-random-jwt-secret-key-at-least-32-characters
AES_KEY=your-32-character-aes-encryption-key

# AWS ECR Registry
ECR_REGISTRY=123456789012.dkr.ecr.ap-northeast-2.amazonaws.com

# Application Configuration
SPRING_PROFILES_ACTIVE=prod
```

### 4.3 배포 스크립트 실행
```bash
# 실행 권한 부여
chmod +x deploy-prod.sh

# 배포 실행
./deploy-prod.sh
```

## 5. Nginx 설정 (Optional - Load Balancer 대신 사용 시)

### 5.1 Nginx 설정 파일 생성
```bash
mkdir -p nginx/ssl
vim nginx/nginx.prod.conf
```

nginx.prod.conf 내용:
```nginx
events {
    worker_connections 1024;
}

http {
    upstream gateway {
        server gateway-service:8000;
    }

    server {
        listen 80;
        server_name api.costwise.site;

        location / {
            proxy_pass http://gateway;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

## 6. 모니터링 및 유지보수

### 6.1 서비스 상태 확인
```bash
# 컨테이너 상태 확인
docker-compose -f docker-compose.prod.yml ps

# 로그 확인 (전체)
docker-compose -f docker-compose.prod.yml logs -f

# 특정 서비스 로그
docker-compose -f docker-compose.prod.yml logs -f backend-service
```

### 6.2 헬스체크
```bash
# Eureka 헬스체크
curl http://localhost:8761/actuator/health

# Gateway 헬스체크
curl http://localhost:8000/actuator/health

# 전체 서비스 헬스체크 스크립트
cat > health-check.sh << 'EOF'
#!/bin/bash
echo "🔍 서비스 헬스체크 시작..."
services=("eureka-service:8761" "gateway-service:8000")
for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    echo -n "Checking $name... "
    if curl -s "http://localhost:$port/actuator/health" | grep -q "UP"; then
        echo "✅ OK"
    else
        echo "❌ Failed"
    fi
done
EOF

chmod +x health-check.sh
./health-check.sh
```

## 7. 자동 재시작 설정

### 7.1 systemd 서비스 생성
```bash
sudo vim /etc/systemd/system/team11-cloud-cost.service
```

서비스 파일 내용:
```ini
[Unit]
Description=Team11 Cloud Cost Service
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/ubuntu/industry-project-02-11/11조/6프로그램
ExecStart=/usr/local/bin/docker-compose -f docker-compose.prod.yml up -d
ExecStop=/usr/local/bin/docker-compose -f docker-compose.prod.yml down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
```

### 7.2 서비스 활성화
```bash
sudo systemctl daemon-reload
sudo systemctl enable team11-cloud-cost.service
sudo systemctl start team11-cloud-cost.service
```

## 8. 백업 및 복원

### 8.1 데이터베이스 백업 스크립트
```bash
cat > backup-db.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/home/ubuntu/backups"
mkdir -p $BACKUP_DIR

# RDS 데이터베이스 백업
PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -U $DB_USERNAME -d team11_cloud_cost > $BACKUP_DIR/db_backup_$DATE.sql

# S3로 업로드 (선택사항)
aws s3 cp $BACKUP_DIR/db_backup_$DATE.sql s3://your-backup-bucket/db-backups/

# 7일 이