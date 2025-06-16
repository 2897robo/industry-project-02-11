#!/bin/bash

echo "🚀 CostWise ECR 빌드 및 푸시 스크립트"
echo "======================================="

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# ECR 레지스트리 URL 설정
if [ -z "$ECR_REGISTRY" ]; then
    echo -e "${YELLOW}ECR 레지스트리 URL을 입력하세요:${NC}"
    echo "예: 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com"
    read ECR_REGISTRY
fi

# 버전 태그
VERSION=${1:-"latest"}

# ECR 로그인
echo -e "${YELLOW}ECR 로그인 중...${NC}"
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ ECR 로그인 실패!${NC}"
    exit 1
fi

# 각 서비스 빌드 및 푸시
services=("eureka-discovery-service" "gateway-service" "auth-service" "user-service" "backend")
ecr_names=("team11-cloud-cost-eureka" "team11-cloud-cost-gateway" "team11-cloud-cost-auth" "team11-cloud-cost-user" "team11-cloud-cost-backend")

echo -e "${YELLOW}서비스 빌드 및 푸시 시작...${NC}"
for i in ${!services[@]}; do
    service=${services[$i]}
    ecr_name=${ecr_names[$i]}
    
    echo -e "${YELLOW}========== $service 처리 중 ==========${NC}"
    cd apps/$service
    
    # Gradle 빌드
    echo "Gradle 빌드 중..."
    ./gradlew clean build -x test
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ $service Gradle 빌드 실패!${NC}"
        exit 1
    fi
    
    # Docker 이미지 빌드
    echo "Docker 이미지 빌드 중..."
    docker build -t $ecr_name:$VERSION .
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ $service Docker 이미지 빌드 실패!${NC}"
        exit 1
    fi
    
    # ECR에 태그 지정 및 푸시
    echo "ECR에 푸시 중..."
    docker tag $ecr_name:$VERSION $ECR_REGISTRY/$ecr_name:$VERSION
    docker push $ECR_REGISTRY/$ecr_name:$VERSION
    
    # latest 태그도 푸시
    if [ "$VERSION" != "latest" ]; then
        docker tag $ecr_name:$VERSION $ECR_REGISTRY/$ecr_name:latest
        docker push $ECR_REGISTRY/$ecr_name:latest
    fi
    
    cd ../..
    echo -e "${GREEN}✅ $service 빌드 및 푸시 완료${NC}"
    echo ""
done

echo -e "${GREEN}✅ 모든 서비스 빌드 및 푸시가 완료되었습니다!${NC}"
echo ""
echo "다음 단계:"
echo "1. EC2 서버에 SSH로 접속"
echo "2. cd /home/ec2-user/app/11조/6프로그램"
echo "3. ./deploy-prod.sh 실행"