# CostWise 백엔드 AWS 배포 가이드

## 🎯 배포 구조
- **프론트엔드**: Vercel (costwise.site, www.costwise.site)
- **백엔드 API**: AWS EC2 (api.costwise.site)

## 📋 필요한 AWS 리소스
1. EC2 인스턴스 (t3.medium)
2. RDS PostgreSQL (db.t3.micro)
3. ElastiCache Redis (cache.t3.micro)
4. ECR 레포지토리 (5개)
5. VPC 및 보안 그룹

## 🚀 Step 1: ECR 레포지토리 생성

```bash
# ECR 레포지토리 생성
aws ecr create-repository --repository-name team11-cloud-cost-backend --region ap-northeast-2
aws ecr create-repository --repository-name team11-cloud-cost-auth --region ap-northeast-2
aws ecr create-repository --repository-name team11-cloud-cost-user --region ap-northeast-2
aws ecr create-repository --repository-name team11-cloud-cost-gateway --region ap-northeast-2
aws ecr create-repository --repository-name team11-cloud-cost-eureka --region ap-northeast-2

# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $(aws sts get-caller-identity --query Account --output text).dkr.ecr.ap-northeast-2.amazonaws.com
```

## 🚀 Step 2: VPC 및 네트워크 설정

### VPC 생성
- CIDR: 10.0.0.0/16
- 리전: ap-northeast-2

### 서브넷 생성
```
- Public Subnet: 10.0.1.0/24 (AZ: ap-northeast-2a)
- Private Subnet A: 10.0.2.0/24 (AZ: ap-northeast-2a) 
- Private Subnet B: 10.0.3.0/24 (AZ: ap-northeast-2c)
```

### 인터넷 게이트웨이
- VPC에 IGW 연결
- Public Subnet 라우팅 테이블에 0.0.0.0/0 → IGW 추가

### 보안 그룹
1. **EC2 보안 그룹** (costwise-ec2-sg)
   - SSH (22): 내 IP
   - HTTP (80): 0.0.0.0/0
   - HTTPS (443): 0.0.0.0/0
   - Custom TCP (8761): 10.0.0.0/16 (Eureka)

2. **RDS 보안 그룹** (costwise-rds-sg)
   - PostgreSQL (5432): costwise-ec2-sg

3. **Redis 보안 그룹** (costwise-redis-sg)
   - Redis (6379): costwise-ec2-sg

## 🚀 Step 3: RDS PostgreSQL 생성

```bash
# RDS 서브넷 그룹 생성 (Private Subnet A, B 사용)

# RDS 인스턴스 생성
- 엔진: PostgreSQL 15.7
- 템플릿: 프리 티어
- DB 인스턴스 식별자: costwise-db
- 마스터 사용자 이름: costwise_admin
- DB 이름: costwise_db
- 인스턴스 클래스: db.t3.micro
- 스토리지: 20GB gp3
- VPC: 위에서 생성한 VPC
- 서브넷 그룹: 위에서 생성한 서브넷 그룹
- 보안 그룹: costwise-rds-sg
```

## 🚀 Step 4: ElastiCache Redis 생성

```bash
# ElastiCache 서브넷 그룹 생성

# Redis 클러스터 생성
- 클러스터 모드: 비활성화
- 노드 유형: cache.t3.micro
- 복제본 수: 0
- 서브넷 그룹: 위에서 생성한 서브넷 그룹
- 보안 그룹: costwise-redis-sg
```

## 🚀 Step 5: EC2 인스턴스 생성 및 설정

### EC2 인스턴스 생성
```bash
- AMI: Amazon Linux 2023
- 인스턴스 유형: t3.medium
- 네트워크: 위에서 생성한 VPC
- 서브넷: Public Subnet
- 퍼블릭 IP 자동 할당: 활성화
- 보안 그룹: costwise-ec2-sg
- 키 페어: 새로 생성 (costwise-key)
- 스토리지: 30GB gp3
```

### Elastic IP 할당
```bash
# Elastic IP 생성 후 EC2 인스턴스에 연결
# 이 IP를 Route 53에서 api.costwise.site에 연결
```

### EC2 초기 설정
```bash
# SSH 접속
ssh -i costwise-key.pem ec2-user@[ELASTIC-IP]

# Git 및 Docker 설치
sudo yum update -y
sudo yum install -y git
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user
sudo chkconfig docker on

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/download/v2.23.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 프로젝트 클론
cd ~
git clone https://github.com/industry-project-02-11/industry-project-02-11.git app
cd app/11조/6프로그램

# 환경 변수 파일 생성
cp .env.prod.example .env
nano .env
```

### .env 파일 설정
```bash
# RDS 엔드포인트 확인 후 입력
DB_URL=jdbc:postgresql://[RDS-ENDPOINT]:5432/costwise_db
DB_USERNAME=costwise_admin
DB_PASSWORD=[RDS-PASSWORD]

# ElastiCache 엔드포인트 확인 후 입력
REDIS_HOST=[REDIS-ENDPOINT]
REDIS_PORT=6379

# 보안 키 생성 (32자 이상)
JWT_SECRET=[생성한-JWT-SECRET]
AES_KEY=[32자-AES-KEY]

# ECR 레지스트리
ECR_REGISTRY=[계정ID].dkr.ecr.ap-northeast-2.amazonaws.com
```

## 🚀 Step 6: SSL 인증서 설정

```bash
# Let's Encrypt 인증서 발급
sudo yum install -y certbot
sudo certbot certonly --standalone -d api.costwise.site --email your-email@example.com

# 인증서 복사
sudo mkdir -p /home/ec2-user/app/11조/6프로그램/nginx/ssl
sudo cp /etc/letsencrypt/live/api.costwise.site/fullchain.pem /home/ec2-user/app/11조/6프로그램/nginx/ssl/
sudo cp /etc/letsencrypt/live/api.costwise.site/privkey.pem /home/ec2-user/app/11조/6프로그램/nginx/ssl/
sudo chown -R ec2-user:ec2-user /home/ec2-user/app/11조/6프로그램/nginx/ssl/

# Cron 자동 갱신 설정
(crontab -l 2>/dev/null; echo "0 0,12 * * * certbot renew --quiet") | crontab -
```

## 🚀 Step 7: 도메인 설정 (Route 53)

가비아에서 구매한 costwise.site 도메인의 네임서버를 Route 53으로 변경:

1. Route 53에서 호스팅 영역 생성 (costwise.site)
2. 가비아에서 네임서버를 Route 53 네임서버로 변경
3. Route 53에서 레코드 생성:
   - api.costwise.site → A 레코드 → EC2 Elastic IP

## 🚀 Step 8: GitHub Actions 설정

Repository Settings → Secrets and variables → Actions:
```
AWS_ACCESS_KEY_ID: [IAM 사용자 액세스 키]
AWS_SECRET_ACCESS_KEY: [IAM 사용자 시크릿 키]
EC2_HOST: [EC2 Elastic IP]
EC2_SSH_KEY: [EC2 프라이빗 키 전체 내용]
```

## 🚀 Step 9: 첫 배포 실행

### 로컬에서 이미지 빌드 및 푸시
```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin [ECR-REGISTRY]

# 이미지 빌드 및 푸시
cd 11조/6프로그램
./build-and-push.sh
```

### EC2에서 배포
```bash
# EC2 접속
ssh -i costwise-key.pem ec2-user@[ELASTIC-IP]

# 배포 실행
cd /home/ec2-user/app/11조/6프로그램
./deploy-prod.sh
```

## ✅ 배포 확인

1. Eureka Dashboard: http://[EC2-IP]:8761
2. API Health Check: https://api.costwise.site/health
3. 서비스 로그 확인: `docker-compose -f docker-compose.prod.yml logs -f`

## 🔧 트러블슈팅

### Docker 권한 문제
```bash
# 재로그인 또는
newgrp docker
```

### 메모리 부족
```bash
# 스왑 메모리 추가
sudo dd if=/dev/zero of=/swapfile bs=128M count=16
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

### 포트 충돌
```bash
# 사용 중인 포트 확인
sudo netstat -tlnp
```