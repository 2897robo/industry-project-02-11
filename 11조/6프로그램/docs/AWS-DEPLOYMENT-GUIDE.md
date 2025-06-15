# AWS 배포 가이드

## 목차
1. [사전 준비사항](#사전-준비사항)
2. [AWS 리소스 생성](#aws-리소스-생성)
3. [ECR 설정 및 이미지 푸시](#ecr-설정-및-이미지-푸시)
4. [EC2 인스턴스 설정](#ec2-인스턴스-설정)
5. [애플리케이션 배포](#애플리케이션-배포)
6. [프론트엔드 배포 (S3 + CloudFront)](#프론트엔드-배포)
7. [도메인 및 SSL 설정](#도메인-및-ssl-설정)
8. [모니터링 설정](#모니터링-설정)

## 사전 준비사항

### 필요한 도구
- AWS CLI 설치 및 설정
- Docker 및 Docker Compose
- Git
- 도메인 (선택사항)

### AWS 계정 설정
```bash
# AWS CLI 설정
aws configure
# AWS Access Key ID: YOUR_ACCESS_KEY
# AWS Secret Access Key: YOUR_SECRET_KEY
# Default region: ap-northeast-2
# Default output format: json
```

## AWS 리소스 생성

### 1. VPC 및 네트워크 설정
```bash
# VPC 생성 (이미 있다면 기존 VPC 사용)
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=team11-vpc}]'

# 서브넷 생성 (Public)
aws ec2 create-subnet --vpc-id vpc-xxx --cidr-block 10.0.1.0/24 --availability-zone ap-northeast-2a
aws ec2 create-subnet --vpc-id vpc-xxx --cidr-block 10.0.2.0/24 --availability-zone ap-northeast-2c

# 서브넷 생성 (Private for RDS)
aws ec2 create-subnet --vpc-id vpc-xxx --cidr-block 10.0.11.0/24 --availability-zone ap-northeast-2a
aws ec2 create-subnet --vpc-id vpc-xxx --cidr-block 10.0.12.0/24 --availability-zone ap-northeast-2c
```

### 2. RDS (PostgreSQL) 생성
```bash
# DB 서브넷 그룹 생성
aws rds create-db-subnet-group \
  --db-subnet-group-name team11-db-subnet \
  --db-subnet-group-description "Subnet group for Team11 RDS" \
  --subnet-ids subnet-xxx subnet-yyy

# RDS 인스턴스 생성
aws rds create-db-instance \
  --db-instance-identifier team11-postgres \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 13.7 \
  --master-username postgres \
  --master-user-password YOUR_STRONG_PASSWORD \
  --allocated-storage 20 \
  --db-subnet-group-name team11-db-subnet \
  --vpc-security-group-ids sg-xxx \
  --no-publicly-accessible
```

### 3. ElastiCache (Redis) 생성
```bash
# Redis 클러스터 생성
aws elasticache create-cache-cluster \
  --cache-cluster-id team11-redis \
  --cache-node-type cache.t3.micro \
  --engine redis \
  --num-cache-nodes 1 \
  --cache-subnet-group-name team11-cache-subnet \
  --security-group-ids sg-xxx
```

### 4. S3 버킷 생성 (프론트엔드용)
```bash
# S3 버킷 생성
aws s3api create-bucket \
  --bucket team11-cloud-cost-frontend \
  --region ap-northeast-2 \
  --create-bucket-configuration LocationConstraint=ap-northeast-2

# 정적 웹 호스팅 설정
aws s3 website s3://team11-cloud-cost-frontend/ \
  --index-document index.html \
  --error-document index.html
```

### 5. ECR 리포지토리 생성
```bash
# ECR 리포지토리 생성
for service in eureka gateway auth user backend; do
  aws ecr create-repository \
    --repository-name team11-cloud-cost-$service \
    --region ap-northeast-2
done
```

## ECR 설정 및 이미지 푸시

### 1. ECR 로그인
```bash
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.ap-northeast-2.amazonaws.com
```

### 2. Docker 이미지 빌드 및 푸시 스크립트
```bash
#!/bin/bash
# build-and-push.sh

ECR_REGISTRY="123456789012.dkr.ecr.ap-northeast-2.amazonaws.com"
SERVICES=("eureka-discovery-service" "gateway-service" "auth-service" "user-service" "backend")

for service in "${SERVICES[@]}"; do
  echo "Building $service..."
  cd apps/$service
  
  # Gradle 빌드
  ./gradlew build -x test
  
  # Docker 이미지 빌드
  docker build -t team11-cloud-cost-${service}:latest .
  
  # 태그 지정
  docker tag team11-cloud-cost-${service}:latest \
    ${ECR_REGISTRY}/team11-cloud-cost-${service}:latest
  
  # ECR로 푸시
  docker push ${ECR_REGISTRY}/team11-cloud-cost-${service}:latest
  
  cd ../..
done
```

## EC2 인스턴스 설정

### 1. EC2 인스턴스 생성
```bash
# EC2 인스턴스 시작
aws ec2 run-instances \
  --image-id ami-0c02fb55956c7d316 \
  --instance-type t3.large \
  --key-name your-key-pair \
  --security-group-ids sg-xxx \
  --subnet-id subnet-xxx \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=team11-app-server}]'
```

### 2. EC2 초기 설정 스크립트
```bash
#!/bin/bash
# EC2에 SSH 접속 후 실행

# 시스템 업데이트
sudo yum update -y

# Docker 설치
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Git 설치
sudo yum install -y git

# 애플리케이션 디렉토리 생성
mkdir -p /home/ec2-user/app
cd /home/ec2-user/app

# 프로젝트 클론
git clone https://github.com/your-repo/team11-project.git .
```

### 3. 환경 변수 설정
```bash
# .env 파일 생성
cat > /home/ec2-user/app/.env << EOF
# Database Configuration
DB_URL=jdbc:postgresql://team11-postgres.xxx.ap-northeast-2.rds.amazonaws.com:5432/team11
DB_USERNAME=postgres
DB_PASSWORD=your_db_password

# Redis Configuration
REDIS_HOST=team11-redis.xxx.cache.amazonaws.com
REDIS_PORT=6379

# Security Keys
JWT_SECRET=your-very-long-random-jwt-secret-key
AES_KEY=your-32-character-aes-encryption-key

# ECR Registry
ECR_REGISTRY=123456789012.dkr.ecr.ap-northeast-2.amazonaws.com
EOF
```

## 애플리케이션 배포

### 1. ECR 로그인 (EC2에서)
```bash
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.ap-northeast-2.amazonaws.com
```

### 2. 애플리케이션 시작
```bash
cd /home/ec2-user/app/11조/6프로그램

# 프로덕션 모드로 실행
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 로그 확인
docker-compose logs -f
```

### 3. 헬스 체크
```bash
# Eureka 확인
curl http://localhost:8761/actuator/health

# Gateway 확인
curl http://localhost:8000/actuator/health

# 각 서비스 확인
for port in 8080 8081 8082; do
  echo "Checking port $port..."
  curl http://localhost:$port/actuator/health
done
```

## 프론트엔드 배포

### 1. 프론트엔드 빌드
```bash
cd apps/front

# 환경 변수 설정
export VITE_API_URL=https://api.team11-cloud-cost.com

# 빌드
npm ci
npm run build
```

### 2. S3에 업로드
```bash
# S3에 빌드 파일 업로드
aws s3 sync dist/ s3://team11-cloud-cost-frontend/ --delete

# 버킷 정책 설정 (public read)
aws s3api put-bucket-policy --bucket team11-cloud-cost-frontend \
  --policy file://bucket-policy.json
```

### 3. CloudFront 배포
```bash
# CloudFront 배포 생성
aws cloudfront create-distribution \
  --distribution-config file://cloudfront-config.json
```

## 도메인 및 SSL 설정

### 1. Route 53 설정
```bash
# 호스팅 영역 생성
aws route53 create-hosted-zone --name team11-cloud-cost.com

# A 레코드 생성 (CloudFront)
aws route53 change-resource-record-sets \
  --hosted-zone-id Z123456789 \
  --change-batch file://route53-records.json
```

### 2. ACM 인증서 발급
```bash
# SSL 인증서 요청
aws acm request-certificate \
  --domain-name team11-cloud-cost.com \
  --subject-alternative-names *.team11-cloud-cost.com \
  --validation-method DNS
```

## 모니터링 설정

### 1. CloudWatch 알람 설정
```bash
# CPU 사용률 알람
aws cloudwatch put-metric-alarm \
  --alarm-name team11-cpu-high \
  --alarm-description "CPU utilization is too high" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2
```

### 2. 로그 그룹 생성
```bash
# CloudWatch 로그 그룹 생성
aws logs create-log-group --log-group-name /aws/team11/application
```

## 보안 그룹 설정

```bash
# 애플리케이션 서버 보안 그룹
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxx \
  --protocol tcp \
  --port 22 \
  --source 0.0.0.0/0  # SSH (제한 필요)

aws ec2 authorize-security-group-ingress \
  --group-id sg-xxx \
  --protocol tcp \
  --port 8000 \
  --source 0.0.0.0/0  # API Gateway

# RDS 보안 그룹
aws ec2 authorize-security-group-ingress \
  --group-id sg-yyy \
  --protocol tcp \
  --port 5432 \
  --source-group sg-xxx  # EC2 보안 그룹
```

## 백업 설정

### 1. RDS 자동 백업
```bash
aws rds modify-db-instance \
  --db-instance-identifier team11-postgres \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00"
```

### 2. S3 버킷 버저닝
```bash
aws s3api put-bucket-versioning \
  --bucket team11-cloud-cost-frontend \
  --versioning-configuration Status=Enabled
```

## 비용 최적화 팁

1. **개발/테스트 환경**
   - t3.micro 인스턴스 사용
   - 사용하지 않을 때는 중지

2. **운영 환경**
   - Reserved Instances 구매 고려
   - Auto Scaling 설정
   - CloudWatch로 리소스 사용률 모니터링

3. **데이터 전송**
   - CloudFront 캐싱 활용
   - 같은 리전 내 통신 유지

## 문제 해결

### Docker 이미지 Pull 실패
```bash
# ECR 로그인 재시도
$(aws ecr get-login --no-include-email --region ap-northeast-2)
```

### 서비스 연결 실패
```bash
# 네트워크 확인
docker network ls
docker network inspect team11-network

# DNS 확인
docker exec -it team11-eureka nslookup eureka-service
```

### 메모리 부족
```bash
# 스왑 메모리 추가
sudo dd if=/dev/zero of=/swapfile bs=1G count=4
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

## 연락처
- 팀장: 김기욱
- 이메일: team11@example.com
- GitHub: https://github.com/industry-project-02-11
