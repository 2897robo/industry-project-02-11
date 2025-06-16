#!/bin/bash

# EC2 인스턴스 생성 스크립트

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
AMI_ID="ami-0ea4d4b8dc1e46212" # Ubuntu 22.04 LTS (ap-northeast-2)

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}    EC2 인스턴스 생성${NC}"
echo -e "${BLUE}===========================================${NC}"

# VPC ID 가져오기
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text --region $REGION)

# 기존 보안 그룹 확인 또는 생성
echo -e "${YELLOW}보안 그룹 설정 중...${NC}"
SG_ID=$(aws ec2 describe-security-groups \
    --filters "Name=group-name,Values=${PROJECT_NAME}-ec2-sg" \
    --query 'SecurityGroups[0].GroupId' \
    --output text \
    --region $REGION 2>/dev/null || echo "None")

if [ "$SG_ID" == "None" ]; then
    # 보안 그룹 생성
    SG_ID=$(aws ec2 create-security-group \
        --group-name "${PROJECT_NAME}-ec2-sg" \
        --description "Security group for EC2 instance" \
        --vpc-id $VPC_ID \
        --region $REGION \
        --query 'GroupId' \
        --output text)
    
    # 보안 그룹 규칙 추가
    # SSH (22)
    aws ec2 authorize-security-group-ingress \
        --group-id $SG_ID \
        --protocol tcp \
        --port 22 \
        --cidr 0.0.0.0/0 \
        --region $REGION
    
    # HTTP (80)
    aws ec2 authorize-security-group-ingress \
        --group-id $SG_ID \
        --protocol tcp \
        --port 80 \
        --cidr 0.0.0.0/0 \
        --region $REGION
    
    # HTTPS (443)
    aws ec2 authorize-security-group-ingress \
        --group-id $SG_ID \
        --protocol tcp \
        --port 443 \
        --cidr 0.0.0.0/0 \
        --region $REGION
    
    # API Gateway (8000)
    aws ec2 authorize-security-group-ingress \
        --group-id $SG_ID \
        --protocol tcp \
        --port 8000 \
        --cidr 0.0.0.0/0 \
        --region $REGION
    
    # Eureka (8761) - 관리용
    aws ec2 authorize-security-group-ingress \
        --group-id $SG_ID \
        --protocol tcp \
        --port 8761 \
        --cidr 0.0.0.0/0 \
        --region $REGION
fi

echo -e "${GREEN}✓ 보안 그룹 준비 완료: ${SG_ID}${NC}"

# 키페어 확인
echo -e "${YELLOW}키페어 확인 중...${NC}"
KEY_EXISTS=$(aws ec2 describe-key-pairs \
    --key-names $KEY_NAME \
    --region $REGION \
    --query 'KeyPairs[0].KeyName' \
    --output text 2>/dev/null || echo "None")

if [ "$KEY_EXISTS" == "None" ]; then
    echo -e "${RED}키페어 ${KEY_NAME}이 존재하지 않습니다.${NC}"
    echo -e "${YELLOW}새 키페어를 생성하시겠습니까? (y/n)${NC}"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        aws ec2 create-key-pair \
            --key-name $KEY_NAME \
            --region $REGION \
            --query 'KeyMaterial' \
            --output text > ${KEY_NAME}.pem
        chmod 400 ${KEY_NAME}.pem
        echo -e "${GREEN}✓ 키페어 생성 완료: ${KEY_NAME}.pem${NC}"
    else
        echo -e "${RED}키페어 없이는 진행할 수 없습니다.${NC}"
        exit 1
    fi
fi

# EC2 인스턴스 생성
echo -e "${YELLOW}EC2 인스턴스 생성 중...${NC}"
INSTANCE_ID=$(aws ec2 run-instances \
    --image-id $AMI_ID \
    --instance-type $INSTANCE_TYPE \
    --key-name $KEY_NAME \
    --security-group-ids $SG_ID \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${PROJECT_NAME}-server}]" \
    --block-device-mappings "DeviceName=/dev/sda1,Ebs={VolumeSize=30,VolumeType=gp3}" \
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

echo -e "${GREEN}✓ 인스턴스 준비 완료!${NC}"
echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}EC2 인스턴스 정보${NC}"
echo -e "${BLUE}===========================================${NC}"
echo -e "인스턴스 ID: ${INSTANCE_ID}"
echo -e "Public IP: ${PUBLIC_IP}"
echo -e "SSH 접속: ssh -i ${KEY_NAME}.pem ubuntu@${PUBLIC_IP}"
echo ""
echo -e "${YELLOW}다음 스크립트를 실행하여 배포를 진행하세요:${NC}"
echo -e "  ${GREEN}./deploy-to-ec2.sh ${PUBLIC_IP}${NC}"