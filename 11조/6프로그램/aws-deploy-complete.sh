#!/bin/bash

# AWS 전체 배포 자동화 스크립트
# Team 11 Cloud Cost Optimization Platform

set -e  # 에러 발생시 중단

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
KEY_NAME="${PROJECT_NAME}-key"
VPC_NAME="${PROJECT_NAME}-vpc"
RDS_INSTANCE="${PROJECT_NAME}-db"
REDIS_CLUSTER="${PROJECT_NAME}-redis"
EC2_INSTANCE_NAME="${PROJECT_NAME}-server"

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}    Team 11 Cloud Cost Platform 배포${NC}"
echo -e "${BLUE}===========================================${NC}"
echo -e "AWS 계정 ID: ${ACCOUNT_ID}"
echo -e "리전: ${REGION}"
echo -e "${BLUE}===========================================${NC}"

# 1. VPC 생성
echo -e "${YELLOW}[1/15] VPC 생성 중...${NC}"
VPC_ID=$(aws ec2 create-vpc \
    --cidr-block 10.0.0.0/16 \
    --tag-specifications "ResourceType=vpc,Tags=[{Key=Name,Value=${VPC_NAME}}]" \
    --region $REGION \
    --query 'Vpc.VpcId' \
    --output text)
echo -e "${GREEN}✓ VPC 생성 완료: ${VPC_ID}${NC}"

# VPC DNS 설정 활성화
aws ec2 modify-vpc-attribute --vpc-id $VPC_ID --enable-dns-support --region $REGION
aws ec2 modify-vpc-attribute --vpc-id $VPC_ID --enable-dns-hostnames --region $REGION

# 2. 서브넷 생성
echo -e "${YELLOW}[2/15] 서브넷 생성 중...${NC}"
# Public Subnet 1
PUBLIC_SUBNET_1=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block 10.0.1.0/24 \
    --availability-zone ${REGION}a \
    --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-public-1}]" \
    --region $REGION \
    --query 'Subnet.SubnetId' \
    --output text)

# Public Subnet 2
PUBLIC_SUBNET_2=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block 10.0.2.0/24 \
    --availability-zone ${REGION}c \
    --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-public-2}]" \
    --region $REGION \
    --query 'Subnet.SubnetId' \
    --output text)

# Private Subnet 1
PRIVATE_SUBNET_1=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block 10.0.11.0/24 \
    --availability-zone ${REGION}a \
    --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-private-1}]" \
    --region $REGION \
    --query 'Subnet.SubnetId' \
    --output text)

# Private Subnet 2
PRIVATE_SUBNET_2=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block 10.0.12.0/24 \
    --availability-zone ${REGION}c \
    --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-private-2}]" \
    --region $REGION \
    --query 'Subnet.SubnetId' \
    --output text)

echo -e "${GREEN}✓ 서브넷 생성 완료${NC}"

# 3. 인터넷 게이트웨이 생성 및 연결
echo -e "${YELLOW}[3/15] 인터넷 게이트웨이 생성 중...${NC}"
IGW_ID=$(aws ec2 create-internet-gateway \
    --tag-specifications "ResourceType=internet-gateway,Tags=[{Key=Name,Value=${PROJECT_NAME}-igw}]" \
    --region $REGION \
    --query 'InternetGateway.InternetGatewayId' \
    --output text)

aws ec2 attach-internet-gateway --vpc-id $VPC_ID --internet-gateway-id $IGW_ID --region $REGION
echo -e "${GREEN}✓ 인터넷 게이트웨이 생성 완료: ${IGW_ID}${NC}"

# 4. 라우트 테이블 설정
echo -e "${YELLOW}[4/15] 라우트 테이블 설정 중...${NC}"
# 메인 라우트 테이블 ID 가져오기
MAIN_ROUTE_TABLE=$(aws ec2 describe-route-tables \
    --filters "Name=vpc-id,Values=$VPC_ID" "Name=association.main,Values=true" \
    --region $REGION \
    --query 'RouteTables[0].RouteTableId' \
    --output text)

# 인터넷 게이트웨이 라우트 추가
aws ec2 create-route \
    --route-table-id $MAIN_ROUTE_TABLE \
    --destination-cidr-block 0.0.0.0/0 \
    --gateway-id $IGW_ID \
    --region $REGION

# Public 서브넷 연결
aws ec2 associate-route-table --subnet-id $PUBLIC_SUBNET_1 --route-table-id $MAIN_ROUTE_TABLE --region $REGION
aws ec2 associate-route-table --subnet-id $PUBLIC_SUBNET_2 --route-table-id $MAIN_ROUTE_TABLE --region $REGION
echo -e "${GREEN}✓ 라우트 테이블 설정 완료${NC}"

# 5. 보안 그룹 생성
echo -e "${YELLOW}[5/15] 보안 그룹 생성 중...${NC}"
# EC2 보안 그룹
EC2_SG=$(aws ec2 create-security-group \
    --group-name "${PROJECT_NAME}-ec2-sg" \
    --description "Security group for EC2 instance" \
    --vpc-id $VPC_ID \
    --region $REGION \
    --query 'GroupId' \
    --output text)

# EC2 보안 그룹 규칙 추가
aws ec2 authorize-security-group-ingress --group-id $EC2_SG --protocol tcp --port 22 --cidr 0.0.0.0/0 --region $REGION
aws ec2 authorize-security-group-ingress --group-id $EC2_SG --protocol tcp --port 80 --cidr 0.0.0.0/0 --region $REGION
aws ec2 authorize-security-group-ingress --group-id $EC2_SG --protocol tcp --port 443 --cidr 0.0.0.0/0 --region $REGION
aws ec2 authorize-security-group-ingress --group-id $EC2_SG --protocol tcp --port 8000 --cidr 0.0.0.0/0 --region $REGION
aws ec2 authorize-security-group-ingress --group-id $EC2_SG --protocol tcp --port 8761 --cidr 0.0.0.0/0 --region $REGION

# RDS 보안 그룹
RDS_SG=$(aws ec2 create-security-group \
    --group-name "${PROJECT_NAME}-rds-sg" \
    --description "Security group for RDS" \
    --vpc-id $VPC_ID \
    --region $REGION \
    --query 'GroupId' \
    --output text)

aws ec2 authorize-security-group-ingress --group-id $RDS_SG --protocol tcp --port 5432 --source-group $EC2_SG --region $REGION

# Redis 보안 그룹
REDIS_SG=$(aws ec2 create-security-group \
    --group-name "${PROJECT_NAME}-redis-sg" \
    --description "Security group for Redis" \
    --vpc-id $VPC_ID \
    --region $REGION \
    --query 'GroupId' \
    --output text)

aws ec2 authorize-security-group-ingress --group-id $REDIS_SG --protocol tcp --port 6379 --source-group $EC2_SG --region $REGION
echo -e "${GREEN}✓ 보안 그룹 생성 완료${NC}"

# 6. RDS 서브넷 그룹 생성
echo -e "${YELLOW}[6/15] RDS 서브넷 그룹 생성 중...${NC}"
aws rds create-db-subnet-group \
    --db-subnet-group-name "${PROJECT_NAME}-db-subnet" \
    --db-subnet-group-description "Subnet group for RDS" \
    --subnet-ids $PRIVATE_SUBNET_1 $PRIVATE_SUBNET_2 \
    --tags "Key=Name,Value=${PROJECT_NAME}-db-subnet" \
    --region $REGION
echo -e "${GREEN}✓ RDS 서브넷 그룹 생성 완료${NC}"

# 7. RDS PostgreSQL 인스턴스 생성
echo -e "${YELLOW}[7/15] RDS PostgreSQL 인스턴스 생성 중... (약 5-10분 소요)${NC}"
DB_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
aws rds create-db-instance \
    --db-instance-identifier $RDS_INSTANCE \
    --db-instance-class db.t3.micro \
    --engine postgres \
    --engine-version "14.9" \
    --master-username postgres \
    --master-user-password $DB_PASSWORD \
    --allocated-storage 20 \
    --vpc-security-group-ids $RDS_SG \
    --db-subnet-group-name "${PROJECT_NAME}-db-subnet" \
    --backup-retention-period 7 \
    --preferred-backup-window "03:00-04:00" \
    --preferred-maintenance-window "Mon:04:00-Mon:05:00" \
    --db-name team11_cloud_cost \
    --storage-type gp3 \
    --no-publicly-accessible \
    --region $REGION

echo -e "${GREEN}✓ RDS 인스턴스 생성 시작됨${NC}"

# 8. ElastiCache 서브넷 그룹 생성
echo -e "${YELLOW}[8/15] ElastiCache 서브넷 그룹 생성 중...${NC}"
aws elasticache create-cache-subnet-group \
    --cache-subnet-group-name "${PROJECT_NAME}-redis-subnet" \
    --cache-subnet-group-description "Subnet group for Redis" \
    --subnet-ids $PRIVATE_SUBNET_1 $PRIVATE_SUBNET_2 \
    --region $REGION
echo -e "${GREEN}✓ ElastiCache 서브넷 그룹 생성 완료${NC}"

# 9. ElastiCache Redis 클러스터 생성
echo -e "${YELLOW}[9/15] ElastiCache Redis 클러스터 생성 중...${NC}"
aws elasticache create-cache-cluster \
    --cache-cluster-id $REDIS_CLUSTER \
    --cache-node-type cache.t3.micro \
    --engine redis \
    --engine-version "7.0" \
    --num-cache-nodes 1 \
    --cache-subnet-group-name "${PROJECT_NAME}-redis-subnet" \
    --security-group-ids $REDIS_SG \
    --region $REGION
echo -e "${GREEN}✓ Redis 클러스터 생성 시작됨${NC}"

# 10. EC2 키 페어 생성
echo -e "${YELLOW}[10/15] EC2 키 페어 생성 중...${NC}"
aws ec2 create-key-pair \
    --key-name $KEY_NAME \
    --query 'KeyMaterial' \
    --output text \
    --region $REGION > ${KEY_NAME}.pem
chmod 600 ${KEY_NAME}.pem
echo -e "${GREEN}✓ 키 페어 생성 완료: ${KEY_NAME}.pem${NC}"

# 11. EC2 User Data 스크립트 생성
echo -e "${YELLOW}[11/15] EC2 User Data 스크립트 생성 중...${NC}"
cat > user-data.sh << 'EOF'
#!/bin/bash
apt-get update
apt-get install -y docker.io docker-compose git

# Docker 서비스 시작
systemctl start docker
systemctl enable docker

# ubuntu 사용자를 docker 그룹에 추가
usermod -aG docker ubuntu

# AWS CLI 설치
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
apt-get install -y unzip
unzip awscliv2.zip
./aws/install

# 프로젝트 클론
cd /home/ubuntu
git clone https://github.com/your-repo/industry-project-02-11.git
chown -R ubuntu:ubuntu industry-project-02-11

# 완료 표시
touch /home/ubuntu/setup-complete
EOF

# Base64 인코딩
USER_DATA=$(base64 -w 0 user-data.sh)

# 12. EC2 인스턴스 생성
echo -e "${YELLOW}[12/15] EC2 인스턴스 생성 중...${NC}"

# Ubuntu 22.04 LTS AMI ID 찾기
AMI_ID=$(aws ec2 describe-images \
    --owners 099720109477 \
    --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" \
    "Name=state,Values=available" \
    --query 'sort_by(Images, &CreationDate)[-1].ImageId' \
    --output text \
    --region $REGION)

echo "Using AMI: $AMI_ID"

INSTANCE_ID=$(aws ec2 run-instances \
    --image-id $AMI_ID \
    --instance-type t3.large \
    --key-name $KEY_NAME \
    --security-group-ids $EC2_SG \
    --subnet-id $PUBLIC_SUBNET_1 \
    --user-data $USER_DATA \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${EC2_INSTANCE_NAME}}]" \
    --iam-instance-profile Name=EC2-ECR-Access \
    --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":30,"VolumeType":"gp3"}}]' \
    --region $REGION \
    --query 'Instances[0].InstanceId' \
    --output text)

echo -e "${GREEN}✓ EC2 인스턴스 생성 완료: ${INSTANCE_ID}${NC}"

# 13. Elastic IP 할당
echo -e "${YELLOW}[13/15] Elastic IP 할당 중...${NC}"
ALLOCATION_ID=$(aws ec2 allocate-address --domain vpc --region $REGION --query 'AllocationId' --output text)
sleep 30  # 인스턴스가 running 상태가 될 때까지 대기
aws ec2 associate-address --instance-id $INSTANCE_ID --allocation-id $ALLOCATION_ID --region $REGION

# Public IP 가져오기
PUBLIC_IP=$(aws ec2 describe-addresses --allocation-ids $ALLOCATION_ID --region $REGION --query 'Addresses[0].PublicIp' --output text)
echo -e "${GREEN}✓ Elastic IP 할당 완료: ${PUBLIC_IP}${NC}"

# 14. RDS 엔드포인트 확인 (생성 완료 대기)
echo -e "${YELLOW}[14/15] RDS 생성 완료 대기 중... (약 5-10분 소요)${NC}"
aws rds wait db-instance-available --db-instance-identifier $RDS_INSTANCE --region $REGION
RDS_ENDPOINT=$(aws rds describe-db-instances \
    --db-instance-identifier $RDS_INSTANCE \
    --region $REGION \
    --query 'DBInstances[0].Endpoint.Address' \
    --output text)
echo -e "${GREEN}✓ RDS 생성 완료: ${RDS_ENDPOINT}${NC}"

# 15. Redis 엔드포인트 확인
echo -e "${YELLOW}[15/15] Redis 엔드포인트 확인 중...${NC}"
sleep 60  # Redis 생성 대기
REDIS_ENDPOINT=$(aws elasticache describe-cache-clusters \
    --cache-cluster-id $REDIS_CLUSTER \
    --show-cache-node-info \
    --region $REGION \
    --query 'CacheClusters[0].CacheNodes[0].Endpoint.Address' \
    --output text)
echo -e "${GREEN}✓ Redis 엔드포인트: ${REDIS_ENDPOINT}${NC}"

# 환경 변수 파일 생성
echo -e "${YELLOW}환경 변수 파일 생성 중...${NC}"
cat > .env.production << EOF
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
ECR_REGISTRY=${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com

# Application Configuration
SPRING_PROFILES_ACTIVE=prod
EOF

# 배포 스크립트 생성
cat > deploy-to-ec2.sh << EOF
#!/bin/bash
# EC2에 환경변수 및 배포 스크립트 전송
scp -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no .env.production ubuntu@${PUBLIC_IP}:/home/ubuntu/
ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@${PUBLIC_IP} << 'ENDSSH'
    cd /home/ubuntu/industry-project-02-11/11조/6프로그램
    cp /home/ubuntu/.env.production .env
    
    # ECR 로그인
    aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com
    
    # Docker Compose 실행
    docker-compose -f docker-compose.prod.yml pull
    docker-compose -f docker-compose.prod.yml up -d
    
    # 헬스체크
    sleep 60
    curl -s http://localhost:8761/actuator/health
ENDSSH
EOF

chmod +x deploy-to-ec2.sh

# 결과 출력
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}🎉 AWS 인프라 구축 완료!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo -e "EC2 Public IP: ${PUBLIC_IP}"
echo -e "RDS Endpoint: ${RDS_ENDPOINT}"
echo -e "Redis Endpoint: ${REDIS_ENDPOINT}"
echo -e "DB Password: ${DB_PASSWORD}"
echo -e ""
echo -e "SSH 접속: ssh -i ${KEY_NAME}.pem ubuntu@${PUBLIC_IP}"
echo -e ""
echo -e "다음 명령어로 애플리케이션 배포:"
echo -e "  ${GREEN}./deploy-to-ec2.sh${NC}"
echo -e ""
echo -e "서비스 URL:"
echo -e "  - API Gateway: http://${PUBLIC_IP}:8000"
echo -e "  - Eureka Dashboard: http://${PUBLIC_IP}:8761"
echo -e "${BLUE}===========================================${NC}"

# 정리
rm -f user-data.sh awscliv2.zip
