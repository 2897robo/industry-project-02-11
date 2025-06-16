#!/bin/bash

# ECR 리포지토리 설정 스크립트

set -e

REGION="ap-northeast-2"
SERVICES=("eureka" "gateway" "auth" "user" "backend")

echo "ECR 리포지토리 확인 및 생성..."

for SERVICE in "${SERVICES[@]}"; do
    REPO_NAME="team11-cloud-cost-${SERVICE}"
    
    # 리포지토리 존재 확인
    if aws ecr describe-repositories --repository-names $REPO_NAME --region $REGION 2>/dev/null; then
        echo "✓ $REPO_NAME 리포지토리가 이미 존재합니다."
    else
        echo "Creating $REPO_NAME repository..."
        aws ecr create-repository \
            --repository-name $REPO_NAME \
            --region $REGION \
            --image-scanning-configuration scanOnPush=true \
            --encryption-configuration encryptionType=AES256
        echo "✓ $REPO_NAME 리포지토리 생성 완료"
    fi
done

# 리포지토리 목록 확인
echo ""
echo "현재 ECR 리포지토리 목록:"
aws ecr describe-repositories --region $REGION --query 'repositories[?contains(repositoryName, `team11`)].repositoryName' --output table
