#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 환경 변수 확인
if [ ! -f .env ]; then
    echo -e "${RED}Error: .env 파일이 없습니다. .env.prod.example을 참고하여 생성해주세요.${NC}"
    exit 1
fi

# 환경 변수 로드
export $(cat .env | grep -v '^#' | xargs)

echo -e "${GREEN}=== Team11 Cloud Cost Optimization Tool 배포 시작 ===${NC}"

# 1. ECR 로그인
echo -e "${YELLOW}1. ECR 로그인 중...${NC}"
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY
if [ $? -ne 0 ]; then
    echo -e "${RED}ECR 로그인 실패${NC}"
    exit 1
fi

# 2. 최신 코드 가져오기
echo -e "${YELLOW}2. 최신 코드 가져오는 중...${NC}"
git pull origin main

# 3. 각 서비스 빌드 및 푸시
SERVICES=("eureka-discovery-service" "gateway-service" "auth-service" "user-service" "backend")

for service in "${SERVICES[@]}"; do
    echo -e "${YELLOW}3. $service 빌드 중...${NC}"
    cd apps/$service
    
    # Gradle 빌드
    ./gradlew clean build -x test
    if [ $? -ne 0 ]; then
        echo -e "${RED}$service 빌드 실패${NC}"
        exit 1
    fi
    
    # Docker 이미지 빌드
    docker build -t team11-cloud-cost-${service}:latest .
    if [ $? -ne 0 ]; then
        echo -e "${RED}$service Docker 이미지 빌드 실패${NC}"
        exit 1
    fi
    
    # 태그 지정 및 푸시
    docker tag team11-cloud-cost-${service}:latest ${ECR_REGISTRY}/team11-cloud-cost-${service}:latest
    docker push ${ECR_REGISTRY}/team11-cloud-cost-${service}:latest
    if [ $? -ne 0 ]; then
        echo -e "${RED}$service 이미지 푸시 실패${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}$service 빌드 및 푸시 완료${NC}"
    cd ../..
done

# 4. 프론트엔드 빌드 및 S3 배포
echo -e "${YELLOW}4. 프론트엔드 빌드 중...${NC}"
cd apps/front
npm ci
npm run build

if [ -n "$S3_BUCKET_NAME" ]; then
    echo -e "${YELLOW}S3에 프론트엔드 배포 중...${NC}"
    aws s3 sync dist/ s3://$S3_BUCKET_NAME/ --delete
    
    if [ -n "$CLOUDFRONT_DISTRIBUTION_ID" ]; then
        echo -e "${YELLOW}CloudFront 캐시 무효화 중...${NC}"
        aws cloudfront create-invalidation --distribution-id $CLOUDFRONT_DISTRIBUTION_ID --paths "/*"
    fi
fi

cd ../..

# 5. 서버에서 컨테이너 업데이트
echo -e "${YELLOW}5. 서버에서 컨테이너 업데이트 중...${NC}"

# 기존 컨테이너 중지 및 제거
docker-compose -f docker-compose.yml -f docker-compose.prod.yml down

# 최신 이미지 가져오기
docker-compose -f docker-compose.yml -f docker-compose.prod.yml pull

# 컨테이너 시작
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 6. 헬스 체크
echo -e "${YELLOW}6. 서비스 헬스 체크 중...${NC}"
sleep 30  # 서비스 시작 대기

# Eureka 체크
curl -f http://localhost:8761/actuator/health > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Eureka Service 정상${NC}"
else
    echo -e "${RED}✗ Eureka Service 오류${NC}"
fi

# Gateway 체크
curl -f http://localhost:8000/actuator/health > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Gateway Service 정상${NC}"
else
    echo -e "${RED}✗ Gateway Service 오류${NC}"
fi

# 기타 서비스 체크
for port in 8080 8081 8082; do
    curl -f http://localhost:$port/actuator/health > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Service on port $port 정상${NC}"
    else
        echo -e "${RED}✗ Service on port $port 오류${NC}"
    fi
done

echo -e "${GREEN}=== 배포 완료 ===${NC}"
echo -e "${YELLOW}로그 확인: docker-compose logs -f${NC}"
