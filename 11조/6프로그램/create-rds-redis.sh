#!/bin/bash

# RDS PostgreSQL 및 ElastiCache Redis 생성 스크립트

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
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text --region $REGION)

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}    RDS 및 Redis 생성${NC}"
echo -e "${BLUE}===========================================${NC}"

# 1. 서브넷 찾기 (최소 2개 필요)
echo -e "${YELLOW}[1/7] 서브넷 확인 중...${NC}"
SUBNET_IDS=$(aws ec2 describe-subnets \
    --filters "Name=vpc-id,Values=$VPC_ID" \
    --query 'Subnets[*].SubnetId' \
    --output text \
    --region $REGION)
SUBNET_ARRAY=($SUBNET_IDS)
echo -e "${GREEN}✓ 서브넷 ${#SUBNET_ARRAY[@]}개 발견${NC}"

# 2. RDS 보안 그룹 생성
echo -e "${YELLOW}[2/7] RDS 보안 그룹 생성 중...${NC}"
RDS_SG=$(aws ec2 create-security-group \
    --group-name "${PROJECT_NAME}-rds-sg" \
    --description "Security group for RDS" \
    --vpc-id $VPC_ID \
    --region $REGION \
    --query 'GroupId' \
    --output text 2>/dev/null || \
    aws ec2 describe-security-groups \
        --filters "Name=group-name,Values=${PROJECT_NAME}-rds-sg" \
        --query 'SecurityGroups[0].GroupId' \
        --output text \
        --region $REGION)

# EC2 보안 그룹 찾기
EC2_SG=$(aws ec2 describe-security-groups \
    --filters "Name=group-name,Values=${PROJECT_NAME}-ec2-sg" \
    --query 'SecurityGroups[0].GroupId' \
    --output text \
    --region $REGION)

# RDS 보안 그룹 규칙 추가
aws ec2 authorize-security-group-ingress \
    --group-id $RDS_SG \
    --protocol tcp \
    --port 5432 \
    --source-group $EC2_SG \
    --region $REGION 2>/dev/null || true

echo -e "${GREEN}✓ RDS 보안 그룹 준비 완료: ${RDS_SG}${NC}"

# 3. Redis 보안 그룹 생성
echo -e "${YELLOW}[3/7] Redis 보안 그룹 생성 중...${NC}"
REDIS_SG=$(aws ec2 create-security-group \
    --group-name "${PROJECT_NAME}-redis-sg" \
    --description "Security group for Redis" \
    --vpc-id $VPC_ID \
    --region $REGION \
    --query 'GroupId' \
    --output text 2>/dev/null || \
    aws ec2 describe-security-groups \
        --filters "Name=group-name,Values=${PROJECT_NAME}-redis-sg" \
        --query 'SecurityGroups[0].GroupId' \
        --output text \
        --region $REGION)

aws ec2 authorize-security-group-ingress \
    --group-id $REDIS_SG \
    --protocol tcp \
    --port 6379 \
    --source-group $EC2_SG \
    --region $REGION 2>/dev/null || true

echo -e "${GREEN}✓ Redis 보안 그룹 준비 완료: ${REDIS_SG}${NC}"

# 4. RDS 서브넷 그룹 생성
echo -e "${YELLOW}[4/7] RDS 서브넷 그룹 생성 중...${NC}"
aws rds create-db-subnet-group \
    --db-subnet-group-name "${PROJECT_NAME}-db-subnet" \
    --db-subnet-group-description "Subnet group for RDS" \
    --subnet-ids ${SUBNET_IDS} \
    --tags "Key=Name,Value=${PROJECT_NAME}-db-subnet" \
    --region $REGION 2>/dev/null || true
echo -e "${GREEN}✓ RDS 서브넷 그룹 준비 완료${NC}"

# 5. RDS PostgreSQL 인스턴스 생성
echo -e "${YELLOW}[5/7] RDS PostgreSQL 인스턴스 생성 중... (약 5-10분 소요)${NC}"
DB_PASSWORD=$(openssl rand -base64 16 | tr -d "=+/" | cut -c1-16)
RDS_INSTANCE="${PROJECT_NAME}-db"

# 기존 인스턴스 확인
if aws rds describe-db-instances --db-instance-identifier $RDS_INSTANCE --region $REGION 2>/dev/null; then
    echo -e "${YELLOW}RDS 인스턴스가 이미 존재합니다${NC}"
    RDS_ENDPOINT=$(aws rds describe-db-instances \
        --db-instance-identifier $RDS_INSTANCE \
        --region $REGION \
        --query 'DBInstances[0].Endpoint.Address' \
        --output text)
else
    aws rds create-db-instance \
        --db-instance-identifier $RDS_INSTANCE \
        --db-instance-class db.t3.micro \
        --engine postgres \
        --engine-version "14.12" \
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
fi

# 6. ElastiCache 서브넷 그룹 생성
echo -e "${YELLOW}[6/7] ElastiCache 서브넷 그룹 생성 중...${NC}"
aws elasticache create-cache-subnet-group \
    --cache-subnet-group-name "${PROJECT_NAME}-redis-subnet" \
    --cache-subnet-group-description "Subnet group for Redis" \
    --subnet-ids ${SUBNET_IDS} \
    --region $REGION 2>/dev/null || true
echo -e "${GREEN}✓ ElastiCache 서브넷 그룹 준비 완료${NC}"

# 7. ElastiCache Redis 클러스터 생성
echo -e "${YELLOW}[7/7] ElastiCache Redis 클러스터 생성 중...${NC}"
REDIS_CLUSTER="${PROJECT_NAME}-redis"

# 기존 클러스터 확인
if aws elasticache describe-cache-clusters --cache-cluster-id $REDIS_CLUSTER --region $REGION 2>/dev/null; then
    echo -e "${YELLOW}Redis 클러스터가 이미 존재합니다${NC}"
    REDIS_ENDPOINT=$(aws elasticache describe-cache-clusters \
        --cache-cluster-id $REDIS_CLUSTER \
        --show-cache-node-info \
        --region $REGION \
        --query 'CacheClusters[0].CacheNodes[0].Endpoint.Address' \
        --output text)
else
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
fi

# 환경 변수 파일 업데이트
echo -e "${YELLOW}환경 변수 파일 업데이트 중...${NC}"

# RDS 생성 대기 (필요한 경우)
if [ -z "$RDS_ENDPOINT" ]; then
    echo "RDS 생성 완료 대기 중... (약 5-10분)"
    aws rds wait db-instance-available --db-instance-identifier $RDS_INSTANCE --region $REGION
    RDS_ENDPOINT=$(aws rds describe-db-instances \
        --db-instance-identifier $RDS_INSTANCE \
        --region $REGION \
        --query 'DBInstances[0].Endpoint.Address' \
        --output text)
fi

# Redis 엔드포인트 확인 (필요한 경우)
if [ -z "$REDIS_ENDPOINT" ]; then
    echo "Redis 생성 대기 중..."
    sleep 60
    REDIS_ENDPOINT=$(aws elasticache describe-cache-clusters \
        --cache-cluster-id $REDIS_CLUSTER \
        --show-cache-node-info \
        --region $REGION \
        --query 'CacheClusters[0].CacheNodes[0].Endpoint.Address' \
        --output text)
fi

# 실제 환경변수 파일 생성
cat > .env.production << EOF
# Database Configuration
DB_URL=jdbc:postgresql://${RDS_ENDPOINT}:5432/team11_cloud_cost
DB_USERNAME=postgres
DB_PASSWORD=${DB_PASSWORD:-기존비밀번호}

# Redis Configuration
REDIS_HOST=${REDIS_ENDPOINT}
REDIS_PORT=6379

# Security Keys
JWT_SECRET=$(openssl rand -base64 32)
AES_KEY=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)

# AWS ECR Registry
ECR_REGISTRY=$(aws sts get-caller-identity --query Account --output text).dkr.ecr.${REGION}.amazonaws.com

# Application Configuration
SPRING_PROFILES_ACTIVE=prod
EOF

# 결과 출력
echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}🎉 RDS 및 Redis 생성 완료!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo -e "RDS 엔드포인트: ${RDS_ENDPOINT}"
echo -e "Redis 엔드포인트: ${REDIS_ENDPOINT}"
if [ ! -z "$DB_PASSWORD" ]; then
    echo -e "DB 비밀번호: ${DB_PASSWORD}"
fi
echo ""
echo -e "환경변수 파일이 생성되었습니다: .env.production"
echo ""
echo -e "다음 명령으로 EC2에 배포하세요:"
echo -e "  ${GREEN}cp .env.production .env.temp && ./deploy-to-ec2.sh${NC}"
echo -e "${BLUE}===========================================${NC}"
