# 🚀 CostWise 배포 체크리스트

## 📋 사전 준비
- [ ] AWS 계정 및 AWS CLI 설정 완료
- [ ] 가비아에서 costwise.site 도메인 구매 완료
- [ ] GitHub 레포지토리 접근 권한 확인

## 1️⃣ AWS 리소스 생성 (콘솔에서)

### VPC 및 네트워크
- [ ] VPC 생성 (10.0.0.0/16)
- [ ] Public Subnet (10.0.1.0/24)
- [ ] Private Subnet A (10.0.2.0/24)
- [ ] Private Subnet B (10.0.3.0/24)
- [ ] 인터넷 게이트웨이 생성 및 연결
- [ ] 라우팅 테이블 설정

### 보안 그룹
- [ ] EC2 보안 그룹 (22, 80, 443, 8761)
- [ ] RDS 보안 그룹 (5432 from EC2)
- [ ] Redis 보안 그룹 (6379 from EC2)

### 데이터베이스
- [ ] RDS PostgreSQL 생성 (db.t3.micro)
- [ ] ElastiCache Redis 생성 (cache.t3.micro)

### 컴퓨팅
- [ ] EC2 인스턴스 생성 (t3.medium)
- [ ] Elastic IP 할당
- [ ] 키 페어 다운로드 및 보관

## 2️⃣ ECR 레포지토리 생성

```bash
# 터미널에서 실행
cd 11조/6프로그램/scripts
./quick-start-aws.sh
```

## 3️⃣ EC2 초기 설정

```bash
# SSH 접속
ssh -i costwise-key.pem ec2-user@[ELASTIC-IP]

# 초기 설정
sudo yum update -y
sudo yum install -y git docker
sudo service docker start
sudo usermod -a -G docker ec2-user
sudo chkconfig docker on

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/download/v2.23.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 재접속
exit
ssh -i costwise-key.pem ec2-user@[ELASTIC-IP]

# 프로젝트 클론
git clone https://github.com/industry-project-02-11/industry-project-02-11.git app
cd app/11조/6프로그램
```

## 4️⃣ 환경 변수 설정

```bash
# .env 파일 생성
cp .env.prod.example .env
nano .env

# 다음 값들을 입력:
# DB_URL=jdbc:postgresql://[RDS-ENDPOINT]:5432/costwise_db
# DB_USERNAME=costwise_admin
# DB_PASSWORD=[설정한 비밀번호]
# REDIS_HOST=[REDIS-ENDPOINT]
# REDIS_PORT=6379
# JWT_SECRET=[32자 이상 랜덤 문자열]
# AES_KEY=[32자 랜덤 문자열]
# ECR_REGISTRY=[계정ID].dkr.ecr.ap-northeast-2.amazonaws.com
```

## 5️⃣ 도메인 설정

### Route 53
- [ ] costwise.site 호스팅 영역 생성
- [ ] 네임서버 확인
- [ ] api.costwise.site → A 레코드 → EC2 Elastic IP

### 가비아
- [ ] 네임서버를 Route 53 네임서버로 변경

### Vercel (프론트엔드)
- [ ] Vercel 프로젝트 생성
- [ ] GitHub 레포지토리 연결
- [ ] 환경 변수 설정 (VITE_API=https://api.costwise.site)
- [ ] costwise.site, www.costwise.site 도메인 연결

## 6️⃣ SSL 인증서 설정

```bash
# EC2에서 실행
sudo yum install -y certbot
sudo certbot certonly --standalone -d api.costwise.site --email admin@costwise.site

# 인증서 복사
sudo mkdir -p /home/ec2-user/app/11조/6프로그램/nginx/ssl
sudo cp /etc/letsencrypt/live/api.costwise.site/fullchain.pem /home/ec2-user/app/11조/6프로그램/nginx/ssl/
sudo cp /etc/letsencrypt/live/api.costwise.site/privkey.pem /home/ec2-user/app/11조/6프로그램/nginx/ssl/
sudo chown -R ec2-user:ec2-user /home/ec2-user/app/11조/6프로그램/nginx/ssl/
```

## 7️⃣ GitHub Actions 설정

Repository Settings → Secrets:
- [ ] AWS_ACCESS_KEY_ID
- [ ] AWS_SECRET_ACCESS_KEY
- [ ] EC2_HOST (Elastic IP)
- [ ] EC2_SSH_KEY (프라이빗 키 전체 내용)

## 8️⃣ 첫 배포

### 로컬에서 이미지 빌드 및 푸시
```bash
# 로컬 터미널에서
cd 11조/6프로그램
ECR_REGISTRY=[ECR레지스트리] ./build-and-push.sh
```

### EC2에서 배포
```bash
# EC2에서
cd /home/ec2-user/app/11조/6프로그램
./deploy-prod.sh
```

## 9️⃣ 배포 확인

- [ ] https://costwise.site 접속 확인
- [ ] https://api.costwise.site/health 확인
- [ ] Eureka Dashboard 확인: http://[EC2-IP]:8761
- [ ] 프론트엔드에서 API 호출 테스트

## 🔧 트러블슈팅

### 메모리 부족 시
```bash
sudo dd if=/dev/zero of=/swapfile bs=128M count=16
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

### 로그 확인
```bash
docker-compose -f docker-compose.prod.yml logs -f [서비스명]
```

## 📞 문제 발생 시
1. CloudWatch 로그 확인
2. docker logs 확인
3. EC2 인스턴스 리소스 모니터링
4. 보안 그룹 규칙 재확인