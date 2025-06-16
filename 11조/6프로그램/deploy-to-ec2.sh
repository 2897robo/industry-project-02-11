#!/bin/bash

# EC2 자동 배포 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 변수 설정
if [ -z "$1" ]; then
    echo -e "${RED}사용법: ./deploy-to-ec2.sh [EC2-PUBLIC-IP]${NC}"
    exit 1
fi

EC2_IP=$1
KEY_FILE="../../costwise-key.pem"
DOMAIN="costwise.site"
REGION="ap-northeast-2"

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}    EC2 자동 배포 시작${NC}"
echo -e "${BLUE}===========================================${NC}"

# RDS 및 Redis 정보 가져오기
echo -e "${YELLOW}AWS 리소스 정보 확인 중...${NC}"

# RDS 엔드포인트
RDS_ENDPOINT=$(aws rds describe-db-instances \
    --db-instance-identifier team11-cloud-cost-db \
    --region $REGION \
    --query 'DBInstances[0].Endpoint.Address' \
    --output text 2>/dev/null || echo "")

# Redis 엔드포인트
REDIS_ENDPOINT=$(aws elasticache describe-cache-clusters \
    --cache-cluster-id team11-cloud-cost-redis \
    --show-cache-node-info \
    --region $REGION \
    --query 'CacheClusters[0].CacheNodes[0].Endpoint.Address' \
    --output text 2>/dev/null || echo "")

# ECR 레지스트리
ECR_REGISTRY=$(aws sts get-caller-identity --query Account --output text).dkr.ecr.${REGION}.amazonaws.com

if [ -z "$RDS_ENDPOINT" ] || [ -z "$REDIS_ENDPOINT" ]; then
    echo -e "${RED}RDS 또는 Redis가 아직 준비되지 않았습니다.${NC}"
    echo "RDS: $RDS_ENDPOINT"
    echo "Redis: $REDIS_ENDPOINT"
    exit 1
fi

echo -e "${GREEN}✓ RDS 엔드포인트: ${RDS_ENDPOINT}${NC}"
echo -e "${GREEN}✓ Redis 엔드포인트: ${REDIS_ENDPOINT}${NC}"
echo -e "${GREEN}✓ ECR 레지스트리: ${ECR_REGISTRY}${NC}"

# 환경변수 파일에서 비밀번호 읽기
if [ -f ".env.production" ]; then
    DB_PASSWORD=$(grep DB_PASSWORD .env.production | cut -d'=' -f2)
else
    echo -e "${YELLOW}DB 비밀번호를 입력하세요:${NC}"
    read -s DB_PASSWORD
fi

# 원격 배포 스크립트 생성
cat > remote-deploy.sh << 'EOF'
#!/bin/bash
set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}시스템 업데이트 중...${NC}"
sudo apt update && sudo apt upgrade -y

echo -e "${YELLOW}필수 패키지 설치 중...${NC}"
sudo apt install -y curl wget git vim htop unzip nginx certbot python3-certbot-nginx

echo -e "${YELLOW}Docker 설치 중...${NC}"
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker ubuntu
    rm get-docker.sh
fi

echo -e "${YELLOW}Docker Compose 설치 중...${NC}"
if ! command -v docker-compose &> /dev/null; then
    sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi

echo -e "${YELLOW}AWS CLI 설치 중...${NC}"
if ! command -v aws &> /dev/null; then
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip -q awscliv2.zip
    sudo ./aws/install
    rm -rf awscliv2.zip aws
fi

echo -e "${GREEN}✓ 시스템 준비 완료${NC}"
EOF

# EC2에 스크립트 복사 및 실행
echo -e "${YELLOW}EC2 서버 초기 설정 중...${NC}"
scp -i $KEY_FILE -o StrictHostKeyChecking=no remote-deploy.sh ubuntu@$EC2_IP:~/
ssh -i $KEY_FILE -o StrictHostKeyChecking=no ubuntu@$EC2_IP "chmod +x remote-deploy.sh && ./remote-deploy.sh"

# 프로젝트 클론 및 환경 설정
echo -e "${YELLOW}프로젝트 배포 중...${NC}"
ssh -i $KEY_FILE ubuntu@$EC2_IP << ENDSSH
set -e

# 프로젝트 클론
if [ ! -d "industry-project-02-11" ]; then
    git clone https://github.com/kookmin-sw/industry-project-02-11.git
fi

cd industry-project-02-11/11조/6프로그램

# 환경변수 파일 생성
cat > .env << 'ENVEOF'
# Database Configuration
DB_URL=jdbc:postgresql://${RDS_ENDPOINT}:5432/team11_cloud_cost
DB_USERNAME=postgres
DB_PASSWORD=${DB_PASSWORD}

# Redis Configuration
REDIS_HOST=${REDIS_ENDPOINT}
REDIS_PORT=6379

# Security Keys
JWT_SECRET=$(openssl rand -base64 32)
AES_KEY=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)

# AWS ECR Registry
ECR_REGISTRY=${ECR_REGISTRY}

# Application Configuration
SPRING_PROFILES_ACTIVE=prod
ENVEOF

# AWS 인증 설정 (EC2 IAM 역할 사용)
aws configure set region ${REGION}

# ECR 로그인
aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}

# Docker 이미지 Pull
docker-compose -f docker-compose.prod.yml pull

# 기존 컨테이너 정지 및 제거
docker-compose -f docker-compose.prod.yml down

# 서비스 시작
docker-compose -f docker-compose.prod.yml up -d

# Nginx 설정
sudo tee /etc/nginx/sites-available/costwise << 'NGINX'
server {
    listen 80;
    server_name ${DOMAIN} api.${DOMAIN};

    location / {
        proxy_pass http://localhost:8000;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        
        # WebSocket 지원
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Eureka Dashboard (선택사항)
    location /eureka/ {
        proxy_pass http://localhost:8761/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}
NGINX

# Nginx 활성화
sudo rm -f /etc/nginx/sites-enabled/default
sudo ln -sf /etc/nginx/sites-available/costwise /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx

# systemd 서비스 생성
sudo tee /etc/systemd/system/team11-cloud-cost.service << 'SYSTEMD'
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
sudo systemctl daemon-reload
sudo systemctl enable team11-cloud-cost

echo "✓ 배포 완료!"
ENDSSH

# 변수 치환
ssh -i $KEY_FILE ubuntu@$EC2_IP "cd industry-project-02-11/11조/6프로그램 && \
    sed -i 's|\${RDS_ENDPOINT}|$RDS_ENDPOINT|g' .env && \
    sed -i 's|\${DB_PASSWORD}|$DB_PASSWORD|g' .env && \
    sed -i 's|\${REDIS_ENDPOINT}|$REDIS_ENDPOINT|g' .env && \
    sed -i 's|\${ECR_REGISTRY}|$ECR_REGISTRY|g' .env && \
    sed -i 's|\${REGION}|$REGION|g' .env && \
    sudo sed -i 's|\${DOMAIN}|$DOMAIN|g' /etc/nginx/sites-available/costwise"

echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}🎉 배포 완료!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""
echo -e "${YELLOW}가비아 DNS 설정:${NC}"
echo -e "1. 가비아 관리 페이지 접속"
echo -e "2. 도메인 관리 → DNS 설정"
echo -e "3. 다음 레코드 추가:"
echo -e "   - 타입: A, 호스트: @, 값: ${EC2_IP}"
echo -e "   - 타입: A, 호스트: api, 값: ${EC2_IP}"
echo -e "   - 타입: A, 호스트: www, 값: ${EC2_IP}"
echo ""
echo -e "${YELLOW}DNS 전파 후 HTTPS 설정:${NC}"
echo -e "ssh -i ${KEY_FILE} ubuntu@${EC2_IP}"
echo -e "sudo certbot --nginx -d ${DOMAIN} -d api.${DOMAIN} -d www.${DOMAIN}"
echo ""
echo -e "${GREEN}서비스 확인:${NC}"
echo -e "- API Gateway: http://${EC2_IP}:8000"
echo -e "- Eureka Dashboard: http://${EC2_IP}:8761"
echo -e "- 도메인 (DNS 설정 후): http://${DOMAIN}"

# 헬스체크
echo -e "\n${YELLOW}서비스 상태 확인 중...${NC}"
sleep 30
curl -s http://${EC2_IP}:8761/actuator/health | grep -q "UP" && \
    echo -e "${GREEN}✓ Eureka 정상 작동${NC}" || \
    echo -e "${RED}✗ Eureka 응답 없음${NC}"

curl -s http://${EC2_IP}:8000/actuator/health | grep -q "UP" && \
    echo -e "${GREEN}✓ Gateway 정상 작동${NC}" || \
    echo -e "${RED}✗ Gateway 응답 없음${NC}"

# 정리
rm -f remote-deploy.sh