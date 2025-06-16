#!/bin/bash

# 빠른 EC2 배포 스크립트 (기존 VPC 및 ECR 이미지 사용)

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
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
KEY_NAME="costwise-key"
EC2_INSTANCE_NAME="${PROJECT_NAME}-server"

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}    Team 11 빠른 EC2 배포${NC}"
echo -e "${BLUE}===========================================${NC}"
echo -e "AWS 계정 ID: ${ACCOUNT_ID}"
echo -e "리전: ${REGION}"
echo -e "${BLUE}===========================================${NC}"

# 1. 기본 VPC 및 서브넷 찾기
echo -e "${YELLOW}[1/8] 기본 VPC 및 서브넷 찾기...${NC}"
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text --region $REGION)
SUBNET_ID=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[0].SubnetId' --output text --region $REGION)
echo -e "${GREEN}✓ VPC: ${VPC_ID}, Subnet: ${SUBNET_ID}${NC}"

# 2. 보안 그룹 생성
echo -e "${YELLOW}[2/8] 보안 그룹 생성 중...${NC}"
EC2_SG=$(aws ec2 create-security-group \
    --group-name "${PROJECT_NAME}-ec2-sg" \
    --description "Security group for EC2 instance" \
    --vpc-id $VPC_ID \
    --region $REGION \
    --query 'GroupId' \
    --output text 2>/dev/null || \
    aws ec2 describe-security-groups \
        --filters "Name=group-name,Values=${PROJECT_NAME}-ec2-sg" \
        --query 'SecurityGroups[0].GroupId' \
        --output text \
        --region $REGION)

# 보안 그룹 규칙 추가 (이미 있으면 무시)
aws ec2 authorize-security-group-ingress --group-id $EC2_SG --protocol tcp --port 22 --cidr 0.0.0.0/0 --region $REGION 2>/dev/null || true
aws ec2 authorize-security-group-ingress --group-id $EC2_SG --protocol tcp --port 80 --cidr 0.0.0.0/0 --region $REGION 2>/dev/null || true
aws ec2 authorize-security-group-ingress --group-id $EC2_SG --protocol tcp --port 443 --cidr 0.0.0.0/0 --region $REGION 2>/dev/null || true
aws ec2 authorize-security-group-ingress --group-id $EC2_SG --protocol tcp --port 8000 --cidr 0.0.0.0/0 --region $REGION 2>/dev/null || true
aws ec2 authorize-security-group-ingress --group-id $EC2_SG --protocol tcp --port 8761 --cidr 0.0.0.0/0 --region $REGION 2>/dev/null || true
echo -e "${GREEN}✓ 보안 그룹 준비 완료: ${EC2_SG}${NC}"

# 3. IAM 역할 확인/생성
echo -e "${YELLOW}[3/8] IAM 역할 확인 중...${NC}"
if ! aws iam get-role --role-name EC2-ECR-Access 2>/dev/null; then
    echo "IAM 역할 생성 중..."
    chmod +x setup-iam-roles.sh
    ./setup-iam-roles.sh
else
    echo -e "${GREEN}✓ IAM 역할이 이미 존재합니다${NC}"
fi

# 4. 최신 Ubuntu AMI 찾기
echo -e "${YELLOW}[4/8] Ubuntu 22.04 AMI 찾기...${NC}"
AMI_ID=$(aws ec2 describe-images \
    --owners 099720109477 \
    --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" \
    "Name=state,Values=available" \
    --query 'sort_by(Images, &CreationDate)[-1].ImageId' \
    --output text \
    --region $REGION)
echo -e "${GREEN}✓ AMI ID: ${AMI_ID}${NC}"

# 5. User Data 스크립트 생성
echo -e "${YELLOW}[5/8] EC2 User Data 스크립트 생성 중...${NC}"
cat > user-data.sh << 'EOF'
#!/bin/bash
apt-get update
apt-get install -y docker.io docker-compose-plugin git awscli

# Docker 서비스 시작
systemctl start docker
systemctl enable docker

# ubuntu 사용자를 docker 그룹에 추가
usermod -aG docker ubuntu

# 완료 표시
touch /home/ubuntu/setup-complete
echo "Setup completed at $(date)" >> /home/ubuntu/setup.log
EOF

# Base64 인코딩 (macOS와 Linux 호환)
if [[ "$OSTYPE" == "darwin"* ]]; then
    USER_DATA=$(base64 -i user-data.sh)
else
    USER_DATA=$(base64 -w 0 user-data.sh)
fi

# 6. EC2 인스턴스 생성
echo -e "${YELLOW}[6/8] EC2 인스턴스 생성 중...${NC}"
INSTANCE_ID=$(aws ec2 run-instances \
    --image-id $AMI_ID \
    --instance-type t3.medium \
    --key-name $KEY_NAME \
    --security-group-ids $EC2_SG \
    --subnet-id $SUBNET_ID \
    --user-data $USER_DATA \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${EC2_INSTANCE_NAME}}]" \
    --iam-instance-profile Name=EC2-ECR-Access \
    --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":30,"VolumeType":"gp3"}}]' \
    --region $REGION \
    --query 'Instances[0].InstanceId' \
    --output text)

echo -e "${GREEN}✓ EC2 인스턴스 생성 시작: ${INSTANCE_ID}${NC}"

# 7. 인스턴스가 실행될 때까지 대기
echo -e "${YELLOW}[7/8] EC2 인스턴스 시작 대기 중...${NC}"
aws ec2 wait instance-running --instance-ids $INSTANCE_ID --region $REGION

# Public IP 가져오기
PUBLIC_IP=$(aws ec2 describe-instances \
    --instance-ids $INSTANCE_ID \
    --query 'Reservations[0].Instances[0].PublicIpAddress' \
    --output text \
    --region $REGION)

echo -e "${GREEN}✓ EC2 인스턴스 실행 중: ${PUBLIC_IP}${NC}"

# 8. 배포 스크립트 생성
echo -e "${YELLOW}[8/8] 배포 스크립트 생성 중...${NC}"

# 임시 환경변수 파일 생성 (실제 값으로 변경 필요)
cat > .env.temp << EOF
# Database Configuration (로컬 테스트용)
DB_URL=jdbc:postgresql://host.docker.internal:5432/team11_cloud_cost
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis Configuration (로컬 테스트용)
REDIS_HOST=host.docker.internal
REDIS_PORT=6379

# Security Keys
JWT_SECRET=$(openssl rand -base64 32)
AES_KEY=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)

# AWS ECR Registry
ECR_REGISTRY=${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com

# Application Configuration
SPRING_PROFILES_ACTIVE=prod
EOF

# 배포 스크립트
cat > deploy-to-ec2.sh << EOF
#!/bin/bash
echo "EC2 배포 시작..."

# SSH 연결 테스트
echo "SSH 연결 대기 중..."
for i in {1..30}; do
    if ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no -o ConnectTimeout=5 ubuntu@${PUBLIC_IP} "echo 'SSH 연결 성공'" 2>/dev/null; then
        break
    fi
    echo "재시도 중... (\$i/30)"
    sleep 10
done

# 프로젝트 클론 및 환경변수 복사
echo "프로젝트 설정 중..."
scp -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no .env.temp ubuntu@${PUBLIC_IP}:/home/ubuntu/.env

ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@${PUBLIC_IP} << 'ENDSSH'
    # Docker 설치 확인
    while ! command -v docker &> /dev/null; do
        echo "Docker 설치 대기 중..."
        sleep 5
    done

    # 프로젝트 클론
    if [ ! -d "industry-project-02-11" ]; then
        git clone https://github.com/industry-project-02-11/industry-project-02-11.git
    fi
    
    cd industry-project-02-11/11조/6프로그램
    
    # 환경변수 파일 복사
    cp /home/ubuntu/.env .env
    
    # ECR 로그인
    aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com
    
    # Docker Compose 실행
    echo "Docker 컨테이너 시작 중..."
    docker compose -f docker-compose.prod.yml pull
    docker compose -f docker-compose.prod.yml up -d
    
    # 상태 확인
    sleep 30
    docker ps
    
    echo "배포 완료!"
ENDSSH
EOF

chmod +x deploy-to-ec2.sh

# 결과 출력
echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}🎉 EC2 인스턴스 생성 완료!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo -e "인스턴스 ID: ${INSTANCE_ID}"
echo -e "Public IP: ${PUBLIC_IP}"
echo -e ""
echo -e "SSH 접속:"
echo -e "  ${GREEN}ssh -i ${KEY_NAME}.pem ubuntu@${PUBLIC_IP}${NC}"
echo -e ""
echo -e "다음 단계:"
echo -e "1. .env.temp 파일을 실제 RDS/Redis 정보로 수정"
echo -e "2. 배포 실행: ${GREEN}./deploy-to-ec2.sh${NC}"
echo -e ""
echo -e "서비스 URL (배포 후):"
echo -e "  - API Gateway: http://${PUBLIC_IP}:8000"
echo -e "  - Eureka Dashboard: http://${PUBLIC_IP}:8761"
echo -e "${BLUE}===========================================${NC}"

# 환경 정보 저장
cat > deployment-info.txt << EOF
배포 정보
========
날짜: $(date)
인스턴스 ID: ${INSTANCE_ID}
Public IP: ${PUBLIC_IP}
보안 그룹: ${EC2_SG}
VPC: ${VPC_ID}
서브넷: ${SUBNET_ID}
EOF

# 정리
rm -f user-data.sh
