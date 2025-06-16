#!/bin/bash

# 추가 서브넷 생성 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 변수 설정
REGION="ap-northeast-2"
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text --region $REGION)
VPC_CIDR=$(aws ec2 describe-vpcs --vpc-ids $VPC_ID --query 'Vpcs[0].CidrBlock' --output text --region $REGION)

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}    추가 서브넷 생성${NC}"
echo -e "${BLUE}===========================================${NC}"

echo -e "${YELLOW}VPC ID: ${VPC_ID}${NC}"
echo -e "${YELLOW}VPC CIDR: ${VPC_CIDR}${NC}"

# 가용 영역 확인
echo -e "${YELLOW}가용 영역 확인 중...${NC}"
AVAILABILITY_ZONES=$(aws ec2 describe-availability-zones --region $REGION --query 'AvailabilityZones[?State==`available`].[ZoneName]' --output text)
AZ_ARRAY=($AVAILABILITY_ZONES)

echo -e "${GREEN}사용 가능한 가용 영역: ${AZ_ARRAY[@]}${NC}"

# 기존 서브넷 확인
EXISTING_SUBNETS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[*].[SubnetId,AvailabilityZone,CidrBlock]' --output text --region $REGION)
echo -e "${YELLOW}기존 서브넷:${NC}"
echo "$EXISTING_SUBNETS"

# ap-northeast-2a에 서브넷 생성 시도
if ! echo "$EXISTING_SUBNETS" | grep -q "ap-northeast-2a"; then
    echo -e "${YELLOW}ap-northeast-2a에 서브넷 생성 중...${NC}"
    NEW_SUBNET_2A=$(aws ec2 create-subnet \
        --vpc-id $VPC_ID \
        --cidr-block 172.31.48.0/20 \
        --availability-zone ap-northeast-2a \
        --region $REGION \
        --query 'Subnet.SubnetId' \
        --output text 2>/dev/null || echo "")
    
    if [ ! -z "$NEW_SUBNET_2A" ]; then
        echo -e "${GREEN}✓ 서브넷 생성 완료: ${NEW_SUBNET_2A}${NC}"
    else
        echo -e "${RED}✗ ap-northeast-2a 서브넷 생성 실패 (CIDR 충돌 가능)${NC}"
    fi
fi

# ap-northeast-2b에 서브넷 생성 시도
if ! echo "$EXISTING_SUBNETS" | grep -q "ap-northeast-2b"; then
    echo -e "${YELLOW}ap-northeast-2b에 서브넷 생성 중...${NC}"
    NEW_SUBNET_2B=$(aws ec2 create-subnet \
        --vpc-id $VPC_ID \
        --cidr-block 172.31.64.0/20 \
        --availability-zone ap-northeast-2b \
        --region $REGION \
        --query 'Subnet.SubnetId' \
        --output text 2>/dev/null || echo "")
    
    if [ ! -z "$NEW_SUBNET_2B" ]; then
        echo -e "${GREEN}✓ 서브넷 생성 완료: ${NEW_SUBNET_2B}${NC}"
    else
        echo -e "${RED}✗ ap-northeast-2b 서브넷 생성 실패 (CIDR 충돌 가능)${NC}"
    fi
fi

# 최종 서브넷 확인
echo -e "${YELLOW}최종 서브넷 목록:${NC}"
aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[*].[SubnetId,AvailabilityZone,CidrBlock]' --output table --region $REGION

# 서브넷 개수 확인
SUBNET_COUNT=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'length(Subnets[*])' --output text --region $REGION)

if [ $SUBNET_COUNT -ge 2 ]; then
    echo -e "${GREEN}✓ RDS 생성에 필요한 서브넷이 준비되었습니다 (${SUBNET_COUNT}개)${NC}"
    echo -e "${YELLOW}이제 다시 RDS 생성 스크립트를 실행하세요:${NC}"
    echo -e "  ${GREEN}./create-rds-redis.sh${NC}"
else
    echo -e "${RED}✗ 서브넷이 부족합니다. 수동으로 생성이 필요합니다.${NC}"
    echo -e "${YELLOW}대안:${NC}"
    echo -e "  1. 다른 리전 사용"
    echo -e "  2. 새 VPC 생성"
    echo -e "  3. AWS 콘솔에서 수동으로 서브넷 생성"
fi