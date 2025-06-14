#!/bin/bash

# 색상 코드 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 서비스 URL 설정
BACKEND_SERVICE_URL="http://localhost:8080"

# 액세스 토큰 (이미 로그인한 경우 사용)
if [ -z "$TOKEN" ]; then
    echo -e "${RED}TOKEN 환경 변수가 설정되지 않았습니다.${NC}"
    echo -e "${YELLOW}먼저 로그인하여 토큰을 설정하세요:${NC}"
    echo "export TOKEN=\"<your-access-token>\""
    exit 1
fi

# AWS 계정 ID (이미 등록된 경우 사용)
if [ -z "$ACCOUNT_ID" ]; then
    echo -e "${YELLOW}AWS 계정 ID를 찾는 중...${NC}"
    AWS_LIST_RESPONSE=$(curl -s -X GET "${BACKEND_SERVICE_URL}/api/aws-accounts" \
        -H "Authorization: Bearer $TOKEN")
    
    ACCOUNT_ID=$(echo "$AWS_LIST_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -z "$ACCOUNT_ID" ]; then
        echo -e "${RED}등록된 AWS 계정이 없습니다.${NC}"
        echo "먼저 AWS 계정을 등록하세요."
        exit 1
    fi
    
    echo -e "${GREEN}AWS 계정 ID 찾음: $ACCOUNT_ID${NC}"
fi

echo -e "${BLUE}=== AWS 데이터 조회 테스트 ===${NC}\n"

# 1. 비용 데이터 조회
echo -e "${BLUE}1. 비용 데이터 조회...${NC}"
COST_DATA=$(curl -s -X GET "${BACKEND_SERVICE_URL}/api/costs/monthly?awsAccountId=${ACCOUNT_ID}" \
    -H "Authorization: Bearer $TOKEN")

if [ -n "$COST_DATA" ] && [ "$COST_DATA" != "[]" ]; then
    echo -e "${GREEN}✓ 월간 비용 데이터:${NC}"
    echo "$COST_DATA" | jq '.'
else
    echo -e "${YELLOW}아직 비용 데이터가 수집되지 않았습니다.${NC}"
fi

# 2. 리소스 목록 조회
echo -e "\n${BLUE}2. AWS 리소스 목록 조회...${NC}"
RESOURCES=$(curl -s -X GET "${BACKEND_SERVICE_URL}/api/resources?awsAccountId=${ACCOUNT_ID}" \
    -H "Authorization: Bearer $TOKEN")

if [ -n "$RESOURCES" ] && [ "$RESOURCES" != "[]" ]; then
    echo -e "${GREEN}✓ 리소스 목록:${NC}"
    echo "$RESOURCES" | jq '.'
else
    echo -e "${YELLOW}아직 리소스 데이터가 수집되지 않았습니다.${NC}"
fi

# 3. 리소스 통계 조회
echo -e "\n${BLUE}3. 리소스 통계 조회...${NC}"
RESOURCE_STATS=$(curl -s -X GET "${BACKEND_SERVICE_URL}/api/resources/statistics?awsAccountId=${ACCOUNT_ID}" \
    -H "Authorization: Bearer $TOKEN")

if [ -n "$RESOURCE_STATS" ]; then
    echo -e "${GREEN}✓ 리소스 통계:${NC}"
    echo "$RESOURCE_STATS" | jq '.'
else
    echo -e "${YELLOW}통계 데이터가 없습니다.${NC}"
fi

# 4. 최적화 추천 조회
echo -e "\n${BLUE}4. 최적화 추천 조회...${NC}"
RECOMMENDATIONS=$(curl -s -X GET "${BACKEND_SERVICE_URL}/api/recommendations?awsAccountId=${ACCOUNT_ID}" \
    -H "Authorization: Bearer $TOKEN")

if [ -n "$RECOMMENDATIONS" ] && [ "$RECOMMENDATIONS" != "[]" ]; then
    echo -e "${GREEN}✓ 최적화 추천:${NC}"
    echo "$RECOMMENDATIONS" | jq '.'
else
    echo -e "${YELLOW}추천 사항이 없습니다.${NC}"
fi

# 5. 알림 조회
echo -e "\n${BLUE}5. 알림 조회...${NC}"
ALERTS=$(curl -s -X GET "${BACKEND_SERVICE_URL}/api/alerts" \
    -H "Authorization: Bearer $TOKEN")

if [ -n "$ALERTS" ] && [ "$ALERTS" != "[]" ]; then
    echo -e "${GREEN}✓ 알림:${NC}"
    echo "$ALERTS" | jq '.'
else
    echo -e "${YELLOW}알림이 없습니다.${NC}"
fi

echo -e "\n${GREEN}=== 데이터 조회 테스트 완료 ===${NC}"
echo -e "${BLUE}참고: 데이터가 없는 경우, AWS API 테스트 스크립트를 실행하여 데이터를 수집하세요.${NC}"
