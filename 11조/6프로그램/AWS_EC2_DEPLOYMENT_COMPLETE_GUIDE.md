# AWS EC2 우분투 배포 완전 가이드

## 현재 상태
- ✅ RDS PostgreSQL 생성 중 (team11-cloud-cost-db)
- ✅ ElastiCache Redis 생성 중 (team11-cloud-cost-redis)
- ✅ ECR에 Docker 이미지 준비됨

## 1. EC2 인스턴스 준비

### 1.1 EC2 인스턴스 생성 (AWS 콘솔)
1. EC2 콘솔에서 "Launch Instance" 클릭
2. 설정:
   - **Name**: team11-cloud-cost-server
   - **AMI**: Ubuntu Server 22.04 LTS (64-bit x86)
   - **Instance type**: t3.large
   - **Key pair**: 기존 키 선택 또는 새로 생성
   - **Network settings**:
     - VPC: 기본 VPC 선택
     - Auto-assign public IP: Enable
     - Security group: team11-cloud-cost-ec2-sg (이미 생성됨)
   - **Storage**: 30 GiB gp3

### 1.2 보안 그룹 확인
기존 보안 그룹(team11-cloud-cost-ec2-sg)에 다음 규칙이 있는지 확인:
```
- SSH (22) - 관리자 IP
- HTTP (80) - 0.0.0.0/0
- HTTPS (443) - 0.0.0.0/0
- Custom TCP (8000) - 0.0.0.0/0 (API Gateway)
- Custom TCP (8761) - 관리자 IP (Eureka Dashboard)
```

## 2. EC2 서버 초기 설정

### 2.1 SSH 접속
```bash
# 로컬에서
ssh -i "costwise-key.pem" ubuntu@[EC2-PUBLIC-IP]
```

### 2.2 시스템 업데이트 및 Docker 설치
```bash
# 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# 필수 패키지 설치
sudo apt install -y curl wget git vim htop unzip

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker ubuntu

# 재로그인 또는 그룹 적용
exit
# 다시 SSH 접속
ssh -i "costwise-key.pem" ubuntu@[EC2-PUBLIC-IP]
```

### 2.3 AWS CLI 설치
```bash
# AWS CLI v2 설치
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# AWS 설정 (IAM 역할 사용 권장)
aws configure
# 입력:
# AWS Access Key ID: [YOUR_ACCESS_KEY]
# AWS Secret Access Key: [YOUR_SECRET_KEY]
# Default region name: ap-northeast-2
# Default output format: json
```

## 3. 프로젝트 배포

### 3.1 프로젝트 클론
```bash
cd ~
git clone https://github.com/[YOUR-REPO]/industry-project-02-11.git
cd industry-project-02-11/11조/6프로그램
```

### 3.2 ECR 로그인
```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin [YOUR-ACCOUNT-ID].dkr.ecr.ap-northeast-2.amazonaws.com
```

### 3.3 RDS 및 Redis 상태 확인
```bash
# RDS 상태 확인
aws rds describe-db-instances \
  --db-instance-identifier team11-cloud-cost-db \
  --query 'DBInstances[0].[DBInstanceStatus,Endpoint.Address]' \
  --output table \
  --region ap-northeast-2

# Redis 상태 확인
aws elasticache describe-cache-clusters \
  --cache-cluster-id team11-cloud-cost-redis \
  --show-cache-node-info \
  --query 'CacheClusters[0].[CacheClusterStatus,CacheNodes[0].Endpoint.Address]' \
  --output table \
  --region ap-northeast-2
```

### 3.4 환경변수 설정
```bash
# .env 파일 생성
cat > .env << EOF
# Database Configuration
DB_URL=jdbc:postgresql://[RDS-ENDPOINT]:5432/team11_cloud_cost
DB_USERNAME=postgres
DB_PASSWORD=[YOUR-DB-PASSWORD]

# Redis Configuration
REDIS_HOST=[REDIS-ENDPOINT]
REDIS_PORT=6379

# Security Keys
JWT_SECRET=$(openssl rand -base64 32)
AES_KEY=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)

# AWS ECR Registry
ECR_REGISTRY=[YOUR-ACCOUNT-ID].dkr.ecr.ap-northeast-2.amazonaws.com

# Application Configuration
SPRING_PROFILES_ACTIVE=prod
EOF

# .env 파일 편집하여 실제 값 입력
vim .env
```

### 3.5 Docker 이미지 Pull
```bash
# ECR에서 이미지 가져오기
docker-compose -f docker-compose.prod.yml pull
```

### 3.6 서비스 시작
```bash
# 서비스 시작
docker-compose -f docker-compose.prod.yml up -d

# 로그 확인
docker-compose -f docker-compose.prod.yml logs -f
```

## 4. 서비스 확인

### 4.1 헬스체크
```bash
# Eureka 확인
curl http://localhost:8761/actuator/health

# Gateway 확인
curl http://localhost:8000/actuator/health

# 브라우저에서 확인
# Eureka Dashboard: http://[EC2-PUBLIC-IP]:8761
# API Gateway: http://[EC2-PUBLIC-IP]:8000
```

### 4.2 서비스 상태 모니터링
```bash
# 컨테이너 상태
docker ps

# 시스템 리소스 확인
htop

# Docker 로그 (특정 서비스)
docker-compose -f docker-compose.prod.yml logs -f gateway-service
```

## 5. 자동 시작 설정

### 5.1 systemd 서비스 생성
```bash
sudo vim /etc/systemd/system/team11-cloud-cost.service
```

내용:
```ini
[Unit]
Description=Team11 Cloud Cost Service
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
User=ubuntu
WorkingDirectory=/home/ubuntu/industry-project-02-11/11조/6프로그램
ExecStart=/usr/local/bin/docker-compose -f docker-compose.prod.yml up -d
ExecStop=/usr/local/bin/docker-compose -f docker-compose.prod.yml down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
```

### 5.2 서비스 활성화
```bash
sudo systemctl daemon-reload
sudo systemctl enable team11-cloud-cost
sudo systemctl start team11-cloud-cost
sudo systemctl status team11-cloud-cost
```

## 6. 도메인 연결 (선택사항)

### 6.1 Route 53 설정
1. Route 53에서 호스팅 영역 생성
2. A 레코드 추가:
   - api.costwise.site → EC2 Public IP

### 6.2 Nginx SSL 설정
```bash
# Certbot 설치
sudo apt install -y certbot python3-certbot-nginx

# SSL 인증서 발급
sudo certbot --nginx -d api.costwise.site
```

## 7. 트러블슈팅

### 문제: 서비스가 시작되지 않음
```bash
# Docker 네트워크 확인
docker network ls

# 컨테이너 로그 확인
docker-compose -f docker-compose.prod.yml logs [service-name]

# 환경변수 확인
cat .env
```

### 문제: 메모리 부족
```bash
# 스왑 파일 생성
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 문제: RDS/Redis 연결 실패
```bash
# 보안 그룹 확인
aws ec2 describe-security-groups --group-ids [SG-ID] --region ap-northeast-2

# 네트워크 연결 테스트
nc -zv [RDS-ENDPOINT] 5432
nc -zv [REDIS-ENDPOINT] 6379
```

## 8. 유지보수

### 백업
```bash
# 데이터베이스 백업 스크립트
cat > backup.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/home/ubuntu/backups"
mkdir -p $BACKUP_DIR

# .env 파일에서 DB 정보 읽기
source .env

# PostgreSQL 백업
PGPASSWORD=$DB_PASSWORD pg_dump -h $(echo $DB_URL | cut -d'/' -f3 | cut -d':' -f1) \
  -U $DB_USERNAME -d team11_cloud_cost > $BACKUP_DIR/db_backup_$DATE.sql

echo "Backup completed: $BACKUP_DIR/db_backup_$DATE.sql"
EOF

chmod +x backup.sh
```

### 로그 로테이션
```bash
# Docker 로그 로테이션 설정
sudo vim /etc/docker/daemon.json
```

내용:
```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```

```bash
sudo systemctl restart docker
```

## 완료!

배포가 완료되었습니다. 다음 URL에서 서비스를 확인할 수 있습니다:
- API Gateway: http://[EC2-PUBLIC-IP]:8000
- Eureka Dashboard: http://[EC2-PUBLIC-IP]:8761

프론트엔드는 Vercel에 별도로 배포되어 있으며, API Gateway를 통해 백엔드와 통신합니다.