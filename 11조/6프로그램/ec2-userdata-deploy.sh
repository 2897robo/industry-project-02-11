#!/bin/bash

# EC2 User Data를 통한 자동 배포 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# AWS 리소스 정보
RDS_ENDPOINT="team11-cloud-cost-db.cvccass28uoc.ap-northeast-2.rds.amazonaws.com"
REDIS_ENDPOINT="team11-cloud-cost-redis.jlfbxu.0001.apn2.cache.amazonaws.com"
ECR_REGISTRY="017820658643.dkr.ecr.ap-northeast-2.amazonaws.com"
DB_PASSWORD="T2GVIc8X3oZ4yBTk"
DOMAIN="costwise.site"
REGION="ap-northeast-2"

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}    EC2 User Data 배포 스크립트 생성${NC}"
echo -e "${BLUE}===========================================${NC}"

# User Data 스크립트 생성
cat > userdata.sh << 'USERDATA'
#!/bin/bash

# 로그 파일 설정
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

echo "========================================"
echo "Team11 Cloud Cost 자동 배포 시작"
echo "========================================"

# 시스템 업데이트
apt-get update
apt-get upgrade -y

# 필수 패키지 설치
apt-get install -y curl wget git vim htop unzip nginx certbot python3-certbot-nginx

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
usermod -aG docker ubuntu

# Docker Compose 설치
curl -L "https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# AWS CLI 설치
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip -q awscliv2.zip
./aws/install
rm -rf awscliv2.zip aws

# 프로젝트 클론
cd /home/ubuntu
sudo -u ubuntu git clone https://github.com/kookmin-sw/industry-project-02-11.git
cd industry-project-02-11/11조/6프로그램

# 환경변수 파일 생성
cat > .env << ENV
# Database Configuration
DB_URL=jdbc:postgresql://RDS_ENDPOINT_PLACEHOLDER:5432/team11_cloud_cost
DB_USERNAME=postgres
DB_PASSWORD=DB_PASSWORD_PLACEHOLDER

# Redis Configuration
REDIS_HOST=REDIS_ENDPOINT_PLACEHOLDER
REDIS_PORT=6379

# Security Keys
JWT_SECRET=$(openssl rand -base64 32)
AES_KEY=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)

# AWS ECR Registry
ECR_REGISTRY=ECR_REGISTRY_PLACEHOLDER

# Application Configuration
SPRING_PROFILES_ACTIVE=prod
ENV

# 플레이스홀더 치환
sed -i "s/RDS_ENDPOINT_PLACEHOLDER/RDS_ENDPOINT_VALUE/g" .env
sed -i "s/DB_PASSWORD_PLACEHOLDER/DB_PASSWORD_VALUE/g" .env
sed -i "s/REDIS_ENDPOINT_PLACEHOLDER/REDIS_ENDPOINT_VALUE/g" .env
sed -i "s/ECR_REGISTRY_PLACEHOLDER/ECR_REGISTRY_VALUE/g" .env

# 소유권 변경
chown -R ubuntu:ubuntu /home/ubuntu/industry-project-02-11

# Docker 그룹 적용을 위한 새 쉘에서 실행
sudo -u ubuntu bash << 'DOCKERSH'
# AWS 설정
aws configure set region REGION_VALUE

# ECR 로그인
aws ecr get-login-password --region REGION_VALUE | docker login --username AWS --password-stdin ECR_REGISTRY_VALUE

# Docker 이미지 Pull
cd /home/ubuntu/industry-project-02-11/11조/6프로그램
docker-compose -f docker-compose.prod.yml pull

# 서비스 시작
docker-compose -f docker-compose.prod.yml up -d
DOCKERSH

# Nginx 설정
cat > /etc/nginx/sites-available/costwise << 'NGINX'
server {
    listen 80;
    server_name DOMAIN_VALUE api.DOMAIN_VALUE www.DOMAIN_VALUE;

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

    # 헬스체크 엔드포인트
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
NGINX

# 플레이스홀더 치환
sed -i "s/DOMAIN_VALUE/DOMAIN_ACTUAL/g" /etc/nginx/sites-available/costwise

# Nginx 활성화
rm -f /etc/nginx/sites-enabled/default
ln -sf /etc/nginx/sites-available/costwise /etc/nginx/sites-enabled/
nginx -t
systemctl restart nginx

# systemd 서비스 생성
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

# 서비스 활성화
systemctl daemon-reload
systemctl enable team11-cloud-cost

echo "========================================"
echo "배포 완료!"
echo "========================================"
USERDATA

# User Data에서 플레이스홀더 치환
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s/RDS_ENDPOINT_VALUE/$RDS_ENDPOINT/g" userdata.sh
    sed -i '' "s/DB_PASSWORD_VALUE/$DB_PASSWORD/g" userdata.sh
    sed -i '' "s/REDIS_ENDPOINT_VALUE/$REDIS_ENDPOINT/g" userdata.sh
    sed -i '' "s/ECR_REGISTRY_VALUE/$ECR_REGISTRY/g" userdata.sh
    sed -i '' "s/REGION_VALUE/$REGION/g" userdata.sh
    sed -i '' "s/DOMAIN_ACTUAL/$DOMAIN/g" userdata.sh
else
    # Linux
    sed -i "s/RDS_ENDPOINT_VALUE/$RDS_ENDPOINT/g" userdata.sh
    sed -i "s/DB_PASSWORD_VALUE/$DB_PASSWORD/g" userdata.sh
    sed -i "s/REDIS_ENDPOINT_VALUE/$REDIS_ENDPOINT/g" userdata.sh
    sed -i "s/ECR_REGISTRY_VALUE/$ECR_REGISTRY/g" userdata.sh
    sed -i "s/REGION_VALUE/$REGION/g" userdata.sh
    sed -i "s/DOMAIN_ACTUAL/$DOMAIN/g" userdata.sh
fi

# Base64 인코딩
USER_DATA_BASE64=$(base64 < userdata.sh)

# 새 인스턴스 생성 또는 기존 인스턴스 재생성
echo -e "${YELLOW}새 EC2 인스턴스를 User Data와 함께 생성하시겠습니까? (y/n)${NC}"
read -r response

if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
    # 기존 인스턴스 종료
    echo -e "${YELLOW}기존 인스턴스 종료 중...${NC}"
    aws ec2 terminate-instances --instance-ids i-0f1519233e15adef9 --region $REGION
    
    # 새 인스턴스 생성
    echo -e "${YELLOW}새 인스턴스 생성 중...${NC}"
    INSTANCE_ID=$(aws ec2 run-instances \
        --image-id ami-03ce92bbf5d824b7b \
        --instance-type t3.large \
        --key-name costwise-key \
        --security-group-ids sg-0ef2b5518a82b7923 \
        --user-data file://userdata.sh \
        --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=team11-cloud-cost-server-auto}]" \
        --block-device-mappings "DeviceName=/dev/sda1,Ebs={VolumeSize=30,VolumeType=gp3}" \
        --iam-instance-profile Name=EC2-ECR-Role \
        --region $REGION \
        --query 'Instances[0].InstanceId' \
        --output text)
    
    echo -e "${GREEN}✓ 새 인스턴스 생성됨: ${INSTANCE_ID}${NC}"
    
    # Public IP 가져오기
    echo -e "${YELLOW}인스턴스 시작 대기 중...${NC}"
    aws ec2 wait instance-running --instance-ids $INSTANCE_ID --region $REGION
    
    PUBLIC_IP=$(aws ec2 describe-instances \
        --instance-ids $INSTANCE_ID \
        --region $REGION \
        --query 'Reservations[0].Instances[0].PublicIpAddress' \
        --output text)
    
    echo ""
    echo -e "${BLUE}===========================================${NC}"
    echo -e "${GREEN}🎉 자동 배포가 시작되었습니다!${NC}"
    echo -e "${BLUE}===========================================${NC}"
    echo -e "인스턴스 ID: ${INSTANCE_ID}"
    echo -e "Public IP: ${PUBLIC_IP}"
    echo ""
    echo -e "${YELLOW}배포 진행 상황 확인 (약 5-10분 소요):${NC}"
    echo -e "1. AWS 콘솔에서 EC2 → 인스턴스 → ${INSTANCE_ID} 선택"
    echo -e "2. 작업 → 모니터링 및 문제 해결 → 시스템 로그 가져오기"
    echo ""
    echo -e "${YELLOW}가비아 DNS 설정:${NC}"
    echo -e "도메인 관리에서 다음 레코드 추가:"
    echo -e "  - A 레코드: @ → ${PUBLIC_IP}"
    echo -e "  - A 레코드: api → ${PUBLIC_IP}"
    echo -e "  - A 레코드: www → ${PUBLIC_IP}"
    echo ""
    echo -e "${YELLOW}배포 완료 후 확인:${NC}"
    echo -e "  - http://${PUBLIC_IP} (바로 확인 가능)"
    echo -e "  - http://api.${DOMAIN} (DNS 설정 후)"
    
    # 정리
    rm -f userdata.sh
else
    echo -e "${YELLOW}User Data 스크립트가 userdata.sh 파일로 저장되었습니다.${NC}"
    echo -e "AWS 콘솔에서 수동으로 인스턴스를 생성할 때 사용하세요."
fi