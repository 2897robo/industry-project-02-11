#!/bin/bash

# Docker 이미지 빌드 및 ECR 푸시 스크립트

set -e

REGION="ap-northeast-2"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_REGISTRY="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

echo "Docker 이미지 빌드 및 ECR 푸시 시작..."

# ECR 로그인
echo "ECR 로그인 중..."
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ECR_REGISTRY

# 서비스 목록
declare -A SERVICES=(
    ["eureka"]="eureka-discovery-service"
    ["gateway"]="gateway-service"
    ["auth"]="auth-service"
    ["user"]="user-service"
    ["backend"]="backend"
)

# 각 서비스 빌드 및 푸시
for SERVICE_NAME in "${!SERVICES[@]}"; do
    SERVICE_DIR="${SERVICES[$SERVICE_NAME]}"
    IMAGE_NAME="${ECR_REGISTRY}/team11-cloud-cost-${SERVICE_NAME}:latest"
    
    echo ""
    echo "Building ${SERVICE_NAME}..."
    
    # 서비스 디렉토리로 이동
    cd apps/${SERVICE_DIR}
    
    # Gradle 빌드
    ./gradlew clean build -x test
    
    # Docker 이미지 빌드
    docker build -t ${IMAGE_NAME} .
    
    # ECR에 푸시
    echo "Pushing ${IMAGE_NAME}..."
    docker push ${IMAGE_NAME}
    
    echo "✓ ${SERVICE_NAME} 완료"
    
    # 원래 디렉토리로 돌아가기
    cd ../..
done

# 프론트엔드 빌드 (필요한 경우)
if [ -d "apps/frontend" ]; then
    echo ""
    echo "Building frontend..."
    cd apps/frontend
    
    # npm 빌드
    npm install
    npm run build
    
    # Docker 이미지 빌드
    IMAGE_NAME="${ECR_REGISTRY}/team11-cloud-cost-frontend:latest"
    docker build -t ${IMAGE_NAME} .
    
    # ECR에 푸시
    echo "Pushing ${IMAGE_NAME}..."
    docker push ${IMAGE_NAME}
    
    echo "✓ frontend 완료"
    
    cd ../..
fi

echo ""
echo "✅ 모든 이미지가 ECR에 푸시되었습니다!"
echo ""
echo "푸시된 이미지:"
aws ecr describe-repositories --region $REGION \
    --query 'repositories[?contains(repositoryName, `team11`)].repositoryUri' \
    --output table
