#!/bin/bash

echo "🚀 CostWise AWS 빠른 시작 가이드"
echo "================================"

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# AWS 계정 ID 가져오기
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text 2>/dev/null)
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ AWS CLI가 설정되지 않았습니다!${NC}"
    echo "aws configure를 실행하여 설정하세요."
    exit 1
fi

echo -e "${GREEN}✅ AWS 계정 ID: $ACCOUNT_ID${NC}"
echo ""

# 리전 설정
REGION="ap-northeast-2"
ECR_REGISTRY="$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"

echo -e "${BLUE}=== Step 1: ECR 레포지토리 생성 ===${NC}"
repos=("team11-cloud-cost-backend" "team11-cloud-cost-auth" "team11-cloud-cost-user" "team11-cloud-cost-gateway" "team11-cloud-cost-eureka")

for repo in "${repos[@]}"; do
    echo -n "Creating $repo... "
    aws ecr create-repository --repository-name $repo --region $REGION >/dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${YELLOW}이미 존재${NC}"
    fi
done

echo ""
echo -e "${BLUE}=== Step 2: 필요한 정보 수집 ===${NC}"
echo -e "${YELLOW}다음 리소스를 AWS 콘솔에서 생성하세요:${NC}"
echo ""
echo "1. VPC 및 서브넷"
echo "   - VPC CIDR: 10.0.0.0/16"
echo "   - Public Subnet: 10.0.1.0/24"
echo "   - Private Subnet A: 10.0.2.0/24"
echo "   - Private Subnet B: 10.0.3.0/24"
echo ""
echo "2. 보안 그룹"
echo "   - EC2: SSH(22), HTTP(80), HTTPS(443), Eureka(8761)"
echo "   - RDS: PostgreSQL(5432) from EC2"
echo "   - Redis: Redis(6379) from EC2"
echo ""
echo "3. RDS PostgreSQL"
echo "   - 엔진: PostgreSQL 15.7"
echo "   - 인스턴스: db.t3.micro"
echo "   - DB 이름: costwise_db"
echo ""
echo "4. ElastiCache Redis"
echo "   - 노드 타입: cache.t3.micro"
echo "   - 클러스터 모드: 비활성화"
echo ""
echo "5. EC2 인스턴스"
echo "   - AMI: Amazon Linux 2023"
echo "   - 타입: t3.medium"
echo "   - 스토리지: 30GB"
echo ""

echo -e "${YELLOW}생성 후 아래 정보를 준비하세요:${NC}"
echo "- RDS 엔드포인트"
echo "- RDS 사용자명/비밀번호"
echo "- Redis 엔드포인트"
echo "- EC2 Elastic IP"
echo ""

echo -e "${BLUE}=== Step 3: .env 파일 템플릿 ===${NC}"
echo "EC2에서 사용할 .env 파일:"
echo ""
cat << EOF
DB_URL=jdbc:postgresql://[RDS-ENDPOINT]:5432/costwise_db
DB_USERNAME=costwise_admin
DB_PASSWORD=[RDS-PASSWORD]
REDIS_HOST=[REDIS-ENDPOINT]
REDIS_PORT=6379
JWT_SECRET=$(openssl rand -base64 32)
AES_KEY=$(openssl rand -base64 24 | head -c 32)
ECR_REGISTRY=$ECR_REGISTRY
EOF

echo ""
echo -e "${BLUE}=== Step 4: GitHub Actions Secrets ===${NC}"
echo "Repository Settings → Secrets에 추가:"
echo ""
echo "AWS_ACCESS_KEY_ID: [IAM 액세스 키]"
echo "AWS_SECRET_ACCESS_KEY: [IAM 시크릿 키]"
echo "EC2_HOST: [EC2 Elastic IP]"
echo "EC2_SSH_KEY: [EC2 프라이빗 키 내용]"
echo ""

echo -e "${BLUE}=== Step 5: 도메인 설정 ===${NC}"
echo "Route 53에서:"
echo "1. costwise.site 호스팅 영역 생성"
echo "2. 가비아에서 네임서버를 Route 53 네임서버로 변경"
echo "3. api.costwise.site → A 레코드 → EC2 Elastic IP"
echo ""

echo -e "${GREEN}✅ 준비 완료!${NC}"
echo ""
echo "다음 명령어로 이미지를 빌드하고 푸시하세요:"
echo -e "${YELLOW}ECR_REGISTRY=$ECR_REGISTRY ./build-and-push.sh${NC}"