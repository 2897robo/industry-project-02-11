#!/bin/bash

# 최종 배포 스크립트 - 안정적인 버전

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 변수 설정
PROJECT_NAME="team11-cloud-cost"
REGION="ap-northeast-2"
INSTANCE_TYPE="t3.large"
KEY_NAME="costwise-key"
AMI_ID="ami-03ce92bbf5d824b7b" # Ubuntu 22.04 LTS
DOMAIN="costwise.site"

# AWS 리소스 정보
RDS_ENDPOINT="team11-cloud-cost-db.cvccass28uoc.ap-northeast-2.rds.amazonaws.com"
REDIS_ENDPOINT="team11-cloud-cost-redis.jlfbxu.0001.apn2.cache.amazonaws.com"
ECR_REGISTRY="017820658643.dkr.ecr.ap-northeast-2.amazonaws.com"
DB_PASSWORD="T2GVIc8X3oZ4yBTk"

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}    최종 EC2 배포 시작${NC}"
echo -e "${BLUE}===========================================${NC}"

# User Data 스크립트 생성
cat > userdata.sh << 'USERDATA'
#!/bin/bash

# 로그 설정
exec > >(tee /var/log/user-data.log)
exec 2>&1

echo "========================================="
echo "Team11 Cloud Cost 배포 시작: $(date)"
echo "========================================="

# 변수 설정
export DEBIAN_FRONTEND=noninteractive
export HOME=/root

# 시스템 업데이트
echo "=== 시스템 업데이트 ==="
apt-get update
apt-get upgrade -y

# 필수 패키지 설치
echo "=== 필수 패키지 설치 ==="
apt-get install -y \
    curl \
    wget \
    git \
    vim \
    htop \
    unzip \
    ca-certificates \
    gnupg \
    lsb-release

# Docker 설치
echo "=== Docker 설치 ==="
mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
usermod -aG docker ubuntu

# Docker Compose 설치
echo "=== Docker Compose 설치 ==="
curl -L "https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# AWS CLI 설치
echo "=== AWS CLI 설치 ==="
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip -q awscliv2.zip
./aws/install
rm -rf awscliv2.zip aws

# 프로젝트 클론
echo "=== 프로젝트 클론 ==="
cd /home/ubuntu
sudo -u ubuntu git clone https://github.com/kookmin-sw/industry-project-02-11.git
cd industry-project-02-11/11조/6프로그램

# 환경변수 파일 생성
echo "=== 환경변수 설정 ==="
cat > .env << 'EOF'
# Database Configuration
DB_URL=jdbc:postgresql://RDS_ENDPOINT:5432/team11_cloud_cost
DB_USERNAME=postgres
DB_PASSWORD=DB_PASSWORD

# Redis Configuration
REDIS_HOST=REDIS_ENDPOINT
REDIS_PORT=6379

# Security Keys
JWT_SECRET=JWT_SECRET_VALUE
AES_KEY=AES_KEY_VALUE

# AWS ECR Registry
ECR_REGISTRY=ECR_REGISTRY_VALUE

# Application Configuration
SPRING_PROFILES_ACTIVE=prod
EOF

# 플레이스홀더 치환
sed -i "s|RDS_ENDPOINT|ACTUAL_RDS_ENDPOINT|g" .env
sed -i "s|DB_PASSWORD|ACTUAL_DB_PASSWORD|g" .env
sed -i "s|REDIS_ENDPOINT|ACTUAL_REDIS_ENDPOINT|g" .env
sed -i "s|ECR_REGISTRY_VALUE|ACTUAL_ECR_REGISTRY|g" .env
sed -i "s|JWT_SECRET_VALUE|$(openssl rand -base64 32)|g" .env
sed -i "s|AES_KEY_VALUE|$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)|g" .env

# 소유권 변경
chown -R ubuntu:ubuntu /home/ubuntu/industry-project-02-11

# AWS 설정
echo "=== AWS 설정 ==="
sudo -u ubuntu aws configure set region REGION_VALUE

# ECR 로그인 및 Docker 실행
echo "=== Docker 컨테이너 시작 ==="
sudo -u ubuntu bash << 'DOCKERBASH'
cd /home/ubuntu/industry-project-02-11/11조/6프로그램

# ECR 로그인
aws ecr get-login-password --region REGION_VALUE | docker login --username AWS --password-stdin ACTUAL_ECR_REGISTRY

# Docker 이미지 Pull
docker-compose -f docker-compose.prod.yml pull

# 서비스 시작
docker-compose -f docker-compose.prod.yml up -d

# 상태 확인
docker-compose -f docker-compose.prod.yml ps
DOCKERBASH

# Nginx 설치 및 설정
echo "=== Nginx 설정 ==="
apt-get install -y nginx

cat > /etc/nginx/sites-available/default << 'NGINX'
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    location / {
        proxy_pass http://localhost:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 지원
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /actuator/health {
        proxy_pass http://localhost:8000/actuator/health;
        access_log off;
    }
}
NGINX

# Nginx 재시작
systemctl restart nginx

# systemd 서비스 생성
echo "=== Systemd 서비스 설정 ==="
cat > /etc/systemd/system/team11-cloud-cost.service << 'SYSTEMD'
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
SYSTEMD

systemctl daemon-reload
systemctl enable team11-cloud-cost

echo "========================================="
echo "배포 완료: $(date)"
echo "========================================="
USERDATA

# 플레이스홀더 치환
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s|ACTUAL_RDS_ENDPOINT|$RDS_ENDPOINT|g" userdata.sh
    sed -i '' "s|ACTUAL_DB_PASSWORD|$DB_PASSWORD|g" userdata.sh
    sed -i '' "s|ACTUAL_REDIS_ENDPOINT|$REDIS_ENDPOINT|g" userdata.sh
    sed -i '' "s|ACTUAL_ECR_REGISTRY|$ECR_REGISTRY|g" userdata.sh
    sed -i '' "s|REGION_VALUE|$REGION|g" userdata.sh
else
    # Linux
    sed -i "s|ACTUAL_RDS_ENDPOINT|$RDS_ENDPOINT|g" userdata.sh
    sed -i "s|ACTUAL_DB_PASSWORD|$DB_PASSWORD|g" userdata.sh
    sed -i "s|ACTUAL_REDIS_ENDPOINT|$REDIS_ENDPOINT|g" userdata.sh
    sed -i "s|ACTUAL_ECR_REGISTRY|$ECR_REGISTRY|g" userdata.sh
    sed -i "s|REGION_VALUE|$REGION|g" userdata.sh
fi

# 인스턴스 생성
echo -e "${YELLOW}새 EC2 인스턴스 생성 중...${NC}"
INSTANCE_ID=$(aws ec2 run-instances \
    --image-id $AMI_ID \
    --instance-type $INSTANCE_TYPE \
    --key-name $KEY_NAME \
    --security-group-ids sg-0ef2b5518a82b7923 \
    --user-data file://userdata.sh \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${PROJECT_NAME}-final}]" \
    --block-device-mappings "DeviceName=/dev/sda1,Ebs={VolumeSize=30,VolumeType=gp3}" \
    --iam-instance-profile Name=EC2-ECR-Role \
    --region $REGION \
    --query 'Instances[0].InstanceId' \
    --output text)

echo -e "${GREEN}✓ 인스턴스 생성됨: ${INSTANCE_ID}${NC}"

# 인스턴스 시작 대기
echo -e "${YELLOW}인스턴스 시작 대기 중...${NC}"
aws ec2 wait instance-running --instance-ids $INSTANCE_ID --region $REGION

# Public IP 가져오기
PUBLIC_IP=$(aws ec2 describe-instances \
    --instance-ids $INSTANCE_ID \
    --region $REGION \
    --query 'Reservations[0].Instances[0].PublicIpAddress' \
    --output text)

echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}🎉 배포가 시작되었습니다!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo -e "인스턴스 ID: ${INSTANCE_ID}"
echo -e "Public IP: ${PUBLIC_IP}"
echo ""
echo -e "${YELLOW}배포 진행 상황 모니터링:${NC}"
echo -e "1. 시스템 로그 확인 (5분 후):"
echo -e "   aws ec2 get-console-output --instance-id ${INSTANCE_ID} --region ${REGION}"
echo ""
echo -e "2. 서비스 상태 확인 (10분 후):"
echo -e "   curl http://${PUBLIC_IP}/actuator/health"
echo -e "   curl http://${PUBLIC_IP}:8761  # Eureka Dashboard"
echo ""
echo -e "${YELLOW}가비아 DNS 설정:${NC}"
echo -e "1. https://www.gabia.com 로그인"
echo -e "2. My 가비아 → 도메인 → DNS 설정"
echo -e "3. 기존 레코드 삭제 후 추가:"
echo -e "   - A 레코드: @ → ${PUBLIC_IP}"
echo -e "   - A 레코드: api → ${PUBLIC_IP}"
echo -e "   - A 레코드: www → ${PUBLIC_IP}"
echo ""
echo -e "${YELLOW}SSL 인증서 설정 (DNS 설정 30분 후):${NC}"
echo -e "ssh -i ../../costwise-key.pem ubuntu@${PUBLIC_IP}"
echo -e "sudo apt install -y certbot python3-certbot-nginx"
echo -e "sudo certbot --nginx -d ${DOMAIN} -d api.${DOMAIN} -d www.${DOMAIN}"

# 정리
rm -f userdata.sh