#!/bin/bash

echo "🚀 Team11 프로덕션 빌드 및 푸시 스크립트"
echo "======================================="

# Docker Hub 사용자명 설정
DOCKER_USERNAME=${1:-"your-docker-username"}
VERSION=${2:-"latest"}

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 각 서비스 빌드 및 태그
services=("eureka-discovery-service" "gateway-service" "auth-service" "user-service" "backend")
service_names=("eureka-service" "gateway-service" "auth-service" "user-service" "backend-service")

echo -e "${YELLOW}1. 서비스 빌드 시작...${NC}"
for i in ${!services[@]}; do
    service=${services[$i]}
    service_name=${service_names[$i]}
    
    echo -e "${YELLOW}Building $service...${NC}"
    cd apps/$service
    
    # Gradle 빌드
    ./gradlew clean build -x test
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ $service 빌드 실패!${NC}"
        exit 1
    fi
    
    # Docker 이미지 빌드
    docker build -t team11/$service_name:$VERSION .
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ $service Docker 이미지 빌드 실패!${NC}"
        exit 1
    fi
    
    # Docker Hub에 태그 지정
    docker tag team11/$service_name:$VERSION $DOCKER_USERNAME/$service_name:$VERSION
    
    cd ../..
    echo -e "${GREEN}✅ $service 빌드 완료${NC}"
done

echo ""
echo -e "${YELLOW}2. Docker Hub에 로그인하세요:${NC}"
echo "docker login"

echo ""
echo -e "${YELLOW}3. 이미지를 푸시하려면 다음 명령어를 실행하세요:${NC}"
for service_name in ${service_names[@]}; do
    echo "docker push $DOCKER_USERNAME/$service_name:$VERSION"
done

echo ""
echo -e "${GREEN}✅ 빌드가 완료되었습니다!${NC}"
echo ""
echo "배포 서버에서 실행할 명령어:"
echo "1. .env 파일 생성 (환경변수 설정)"
echo "2. docker-compose.prod.yml 파일에서 이미지 이름 업데이트"
echo "3. docker-compose -f docker-compose.prod.yml pull"
echo "4. docker-compose -f docker-compose.prod.yml up -d"
