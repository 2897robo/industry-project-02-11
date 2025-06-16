#!/bin/bash

echo "🚀 CostWise EC2 인스턴스 자동 생성 스크립트"
echo "=========================================="

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# 변수 설정
REGION="ap-northeast-2"
KEY_NAME="costwise-key"
INSTANCE_TYPE="t3.medium"
AMI_ID="ami-0c0d141b3ade8438d"  # Amazon Linux 2023 AMI (서울 리전)
SECURITY_GROUP_NAME="costwise-ec2-sg"

# AWS 계정 확인
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text 2>/dev/null)
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ AWS CLI가 설정되지 않았습니다!${NC}"
    echo "aws configure를 실행하여 설정하세요."
    exit 1
fi

echo -e "${GREEN}✅ AWS 계정 ID: $ACCOUNT_ID${NC}"
echo ""

# 1. VPC 확인 또는 생성
echo -e "${BLUE}1. VPC 확인...${NC}"
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=tag:Name,Values=costwise-vpc" --query 'Vpcs[0].VpcId' --output text --region $REGION 2>/dev/null)

if [ "$VPC_ID" == "None" ] || [ -z "$VPC_ID" ]; then
    echo "VPC 생성 중..."
    VPC_ID=$(aws ec2 create-vpc --cidr-block 10.0.0.0/16 --tag-specifications "ResourceType=vpc,Tags=[{Key=Name,Value=costwise-vpc}]" --query 'Vpc.VpcId' --output text --region $REGION)
    echo -e "${GREEN}✅ VPC 생성됨: $VPC_ID${NC}"
    
    # DNS 호스트네임 활성화
    aws ec2 modify-vpc-attribute --vpc-id $VPC_ID --enable-dns-hostnames --region $REGION
else
    echo -e "${GREEN}✅ 기존 VPC 사용: $VPC_ID${NC}"
fi

# 2. 인터넷 게이트웨이 확인 또는 생성
echo -e "${BLUE}2. 인터넷 게이트웨이 확인...${NC}"
IGW_ID=$(aws ec2 describe-internet-gateways --filters "Name=tag:Name,Values=costwise-igw" --query 'InternetGateways[0].InternetGatewayId' --output text --region $REGION 2>/dev/null)

if [ "$IGW_ID" == "None" ] || [ -z "$IGW_ID" ]; then
    echo "인터넷 게이트웨이 생성 중..."
    IGW_ID=$(aws ec2 create-internet-gateway --tag-specifications "ResourceType=internet-gateway,Tags=[{Key=Name,Value=costwise-igw}]" --query 'InternetGateway.InternetGatewayId' --output text --region $REGION)
    aws ec2 attach-internet-gateway --internet-gateway-id $IGW_ID --vpc-id $VPC_ID --region $REGION
    echo -e "${GREEN}✅ 인터넷 게이트웨이 생성됨: $IGW_ID${NC}"
else
    echo -e "${GREEN}✅ 기존 인터넷 게이트웨이 사용: $IGW_ID${NC}"
fi

# 3. 서브넷 확인 또는 생성
echo -e "${BLUE}3. 퍼블릭 서브넷 확인...${NC}"
SUBNET_ID=$(aws ec2 describe-subnets --filters "Name=tag:Name,Values=costwise-public-subnet" --query 'Subnets[0].SubnetId' --output text --region $REGION 2>/dev/null)

if [ "$SUBNET_ID" == "None" ] || [ -z "$SUBNET_ID" ]; then
    echo "퍼블릭 서브넷 생성 중..."
    SUBNET_ID=$(aws ec2 create-subnet --vpc-id $VPC_ID --cidr-block 10.0.1.0/24 --availability-zone ${REGION}a --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=costwise-public-subnet}]" --query 'Subnet.SubnetId' --output text --region $REGION)
    
    # 퍼블릭 IP 자동 할당 활성화
    aws ec2 modify-subnet-attribute --subnet-id $SUBNET_ID --map-public-ip-on-launch --region $REGION
    echo -e "${GREEN}✅ 퍼블릭 서브넷 생성됨: $SUBNET_ID${NC}"
else
    echo -e "${GREEN}✅ 기존 퍼블릭 서브넷 사용: $SUBNET_ID${NC}"
fi

# 4. 라우팅 테이블 설정
echo -e "${BLUE}4. 라우팅 테이블 설정...${NC}"
ROUTE_TABLE_ID=$(aws ec2 describe-route-tables --filters "Name=vpc-id,Values=$VPC_ID" "Name=association.main,Values=true" --query 'RouteTables[0].RouteTableId' --output text --region $REGION)
aws ec2 create-route --route-table-id $ROUTE_TABLE_ID --destination-cidr-block 0.0.0.0/0 --gateway-id $IGW_ID --region $REGION 2>/dev/null
echo -e "${GREEN}✅ 라우팅 테이블 설정 완료${NC}"

# 5. 보안 그룹 확인 또는 생성
echo -e "${BLUE}5. 보안 그룹 확인...${NC}"
SG_ID=$(aws ec2 describe-security-groups --filters "Name=group-name,Values=$SECURITY_GROUP_NAME" "Name=vpc-id,Values=$VPC_ID" --query 'SecurityGroups[0].GroupId' --output text --region $REGION 2>/dev/null)

if [ "$SG_ID" == "None" ] || [ -z "$SG_ID" ]; then
    echo "보안 그룹 생성 중..."
    SG_ID=$(aws ec2 create-security-group --group-name $SECURITY_GROUP_NAME --description "CostWise EC2 Security Group" --vpc-id $VPC_ID --query 'GroupId' --output text --region $REGION)
    
    # 보안 그룹 규칙 추가
    aws ec2 authorize-security-group-ingress --group-id $SG_ID --protocol tcp --port 22 --cidr 0.0.0.0/0 --region $REGION
    aws ec2 authorize-security-group-ingress --group-id $SG_ID --protocol tcp --port 80 --cidr 0.0.0.0/0 --region $REGION
    aws ec2 authorize-security-group-ingress --group-id $SG_ID --protocol tcp --port 443 --cidr 0.0.0.0/0 --region $REGION
    aws ec2 authorize-security-group-ingress --group-id $SG_ID --protocol tcp --port 8761 --cidr 10.0.0.0/16 --region $REGION
    
    echo -e "${GREEN}✅ 보안 그룹 생성됨: $SG_ID${NC}"
else
    echo -e "${GREEN}✅ 기존 보안 그룹 사용: $SG_ID${NC}"
fi

# 6. 키 페어 확인 또는 생성
echo -e "${BLUE}6. 키 페어 확인...${NC}"
KEY_EXISTS=$(aws ec2 describe-key-pairs --key-names $KEY_NAME --query 'KeyPairs[0].KeyName' --output text --region $REGION 2>/dev/null)

if [ "$KEY_EXISTS" == "None" ] || [ -z "$KEY_EXISTS" ]; then
    echo "키 페어 생성 중..."
    aws ec2 create-key-pair --key-name $KEY_NAME --query 'KeyMaterial' --output text --region $REGION > ${KEY_NAME}.pem
    chmod 400 ${KEY_NAME}.pem
    echo -e "${GREEN}✅ 키 페어 생성됨: ${KEY_NAME}.pem${NC}"
    echo -e "${YELLOW}⚠️  ${KEY_NAME}.pem 파일을 안전한 곳에 보관하세요!${NC}"
else
    echo -e "${YELLOW}⚠️  기존 키 페어 사용: $KEY_NAME${NC}"
    echo -e "${YELLOW}   만약 키 파일이 없다면 키 페어를 삭제하고 다시 실행하세요.${NC}"
fi

# 7. EC2 인스턴스 생성
echo -e "${BLUE}7. EC2 인