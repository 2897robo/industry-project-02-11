#!/bin/bash

# IAM 역할 생성 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}    EC2 IAM 역할 생성${NC}"
echo -e "${BLUE}===========================================${NC}"

# 역할 이름
ROLE_NAME="EC2-ECR-Role"
INSTANCE_PROFILE_NAME="EC2-ECR-Role"

# 신뢰 정책 문서 생성
cat > trust-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# IAM 역할 생성
echo -e "${YELLOW}IAM 역할 생성 중...${NC}"
aws iam create-role \
    --role-name $ROLE_NAME \
    --assume-role-policy-document file://trust-policy.json \
    2>/dev/null || echo -e "${YELLOW}역할이 이미 존재합니다.${NC}"

# ECR 정책 연결
echo -e "${YELLOW}ECR 정책 연결 중...${NC}"
aws iam attach-role-policy \
    --role-name $ROLE_NAME \
    --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly \
    2>/dev/null || true

# CloudWatch Logs 정책 연결 (선택사항)
aws iam attach-role-policy \
    --role-name $ROLE_NAME \
    --policy-arn arn:aws:iam::aws:policy/CloudWatchLogsFullAccess \
    2>/dev/null || true

# 인스턴스 프로파일 생성
echo -e "${YELLOW}인스턴스 프로파일 생성 중...${NC}"
aws iam create-instance-profile \
    --instance-profile-name $INSTANCE_PROFILE_NAME \
    2>/dev/null || echo -e "${YELLOW}인스턴스 프로파일이 이미 존재합니다.${NC}"

# 역할을 인스턴스 프로파일에 추가
aws iam add-role-to-instance-profile \
    --instance-profile-name $INSTANCE_PROFILE_NAME \
    --role-name $ROLE_NAME \
    2>/dev/null || true

echo -e "${GREEN}✓ IAM 역할 생성 완료!${NC}"
echo -e "역할 이름: ${ROLE_NAME}"
echo -e "인스턴스 프로파일: ${INSTANCE_PROFILE_NAME}"

# 정리
rm -f trust-policy.json