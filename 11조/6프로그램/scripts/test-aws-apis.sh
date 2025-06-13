#!/bin/bash

# 색상 코드 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 서비스 URL 설정
USER_SERVICE_URL="http://localhost:8081"
AUTH_SERVICE_URL="http://localhost:8082"
BACKEND_SERVICE_URL="http://localhost:8080"

# 테스트 사용자 정보
TEST_UID="test_user_aws"
TEST_PASSWORD="password123"
TEST_EMAIL="test@example.com"
TEST_NAME="Test User"

# AWS 계정 정보 (제공된 정보 사용)
AWS_ACCOUNT_ID="017820658643"
AWS_ACCESS_KEY_ID="AKIAQIJRRVPJQS3W53QX"
AWS_SECRET_ACCESS_KEY="zYTvnEk5vpQlWypIsev/1JKCeEX/VMLESmPxkFWi"
AWS_REGION="ap-northeast-2"

echo -e "${BLUE}=== AWS API 테스트 시작 ===${NC}\n"

# 1. 사용자 중복 확인
echo -e "${BLUE}1. 사용자 중복 확인...${NC}"
DUPLICATE_CHECK=$(curl -s -X GET "${USER_SERVICE_URL}/users/check?uid=${TEST_UID}")
echo "중복 확인 결과: $DUPLICATE_CHECK"

# 2. 사용자 생성 (이미 존재하지 않는 경우)
if [ "$DUPLICATE_CHECK" = "false" ]; then
    echo -e "\n${BLUE}2. 새 사용자 생성...${NC}"
    CREATE_USER_RESPONSE=$(curl -s -X POST "${USER_SERVICE_URL}/users" \
        -H "Content-Type: application/json" \
        -d "{
            \"uid\": \"${TEST_UID}\",
            \"password\": \"${TEST_PASSWORD}\",
            \"email\": \"${TEST_EMAIL}\",
            \"name\": \"${TEST_NAME}\"
        }" \
        -w "\nHTTP_STATUS:%{http_code}")
    
    HTTP_STATUS=$(echo "$CREATE_USER_RESPONSE" | grep "HTTP_STATUS" | cut -d':' -f2)
    if [ "$HTTP_STATUS" = "204" ]; then
        echo -e "${GREEN}✓ 사용자 생성 성공${NC}"
    else
        echo -e "${RED}✗ 사용자 생성 실패: $CREATE_USER_RESPONSE${NC}"
    fi
else
    echo -e "${GREEN}✓ 사용자가 이미 존재합니다${NC}"
fi

# 3. 로그인하여 액세스 토큰 획득
echo -e "\n${BLUE}3. 로그인...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "${AUTH_SERVICE_URL}/auth/login" \
    -H "Content-Type: application/json" \
    -d "{
        \"uid\": \"${TEST_UID}\",
        \"password\": \"${TEST_PASSWORD}\"
    }")

ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -n "$ACCESS_TOKEN" ]; then
    echo -e "${GREEN}✓ 로그인 성공${NC}"
    echo "액세스 토큰: ${ACCESS_TOKEN:0:50}..."
else
    echo -e "${RED}✗ 로그인 실패: $LOGIN_RESPONSE${NC}"
    exit 1
fi

# 4. AWS 계정 등록
echo -e "\n${BLUE}4. AWS 계정 등록...${NC}"
AWS_ACCOUNT_RESPONSE=$(curl -s -X POST "${BACKEND_SERVICE_URL}/api/aws-accounts" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"accountAlias\": \"cloud-cost-test\",
        \"awsAccountId\": \"${AWS_ACCOUNT_ID}\",
        \"accessKeyId\": \"${AWS_ACCESS_KEY_ID}\",
        \"secretAccessKey\": \"${AWS_SECRET_ACCESS_KEY}\",
        \"region\": \"${AWS_REGION}\"
    }" \
    -w "\nHTTP_STATUS:%{http_code}")

HTTP_STATUS=$(echo "$AWS_ACCOUNT_RESPONSE" | grep "HTTP_STATUS" | cut -d':' -f2)
RESPONSE_BODY=$(echo "$AWS_ACCOUNT_RESPONSE" | sed '$d')

if [ "$HTTP_STATUS" = "201" ]; then
    echo -e "${GREEN}✓ AWS 계정 등록 성공${NC}"
    echo "응답: $RESPONSE_BODY"
    ACCOUNT_ID=$(echo "$RESPONSE_BODY" | grep -o '"id":[0-9]*' | cut -d':' -f2)
else
    # 이미 등록된 경우 목록에서 ID 가져오기
    echo "AWS 계정 등록 응답: $RESPONSE_BODY"
    echo -e "\n${BLUE}기존 AWS 계정 목록 조회...${NC}"
    
    AWS_LIST_RESPONSE=$(curl -s -X GET "${BACKEND_SERVICE_URL}/api/aws-accounts" \
        -H "Authorization: Bearer $ACCESS_TOKEN")
    
    echo "AWS 계정 목록: $AWS_LIST_RESPONSE"
    ACCOUNT_ID=$(echo "$AWS_LIST_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
fi

if [ -z "$ACCOUNT_ID" ]; then
    echo -e "${RED}✗ AWS 계정 ID를 찾을 수 없습니다${NC}"
    exit 1
fi

echo -e "${GREEN}AWS 계정 ID: $ACCOUNT_ID${NC}"

# 5. AWS 계정 정보 조회
echo -e "\n${BLUE}5. AWS 계정 정보 조회...${NC}"
AWS_ACCOUNT_INFO=$(curl -s -X GET "${BACKEND_SERVICE_URL}/api/aws-accounts/${ACCOUNT_ID}" \
    -H "Authorization: Bearer $ACCESS_TOKEN")

echo "AWS 계정 정보: $AWS_ACCOUNT_INFO"

# 6. 리소스 수집 테스트
echo -e "\n${BLUE}6. AWS 리소스 수집 시작...${NC}"
COLLECT_RESOURCES_RESPONSE=$(curl -s -X POST "${BACKEND_SERVICE_URL}/api/aws-data/collect-resources/${ACCOUNT_ID}" \
    -H "Authorization: Bearer $ACCESS_TOKEN")

echo "리소스 수집 응답: $COLLECT_RESOURCES_RESPONSE"

# 7. 비용 데이터 수집 테스트
echo -e "\n${BLUE}7. AWS 비용 데이터 수집 시작...${NC}"
COLLECT_COSTS_RESPONSE=$(curl -s -X POST "${BACKEND_SERVICE_URL}/api/aws-data/collect-costs/${ACCOUNT_ID}" \
    -H "Authorization: Bearer $ACCESS_TOKEN")

echo "비용 수집 응답: $COLLECT_COSTS_RESPONSE"

# 8. 메트릭 업데이트 테스트
echo -e "\n${BLUE}8. AWS 메트릭 업데이트 시작...${NC}"
UPDATE_METRICS_RESPONSE=$(curl -s -X POST "${BACKEND_SERVICE_URL}/api/aws-data/update-metrics/${ACCOUNT_ID}" \
    -H "Authorization: Bearer $ACCESS_TOKEN")

echo "메트릭 업데이트 응답: $UPDATE_METRICS_RESPONSE"

# 9. 전체 데이터 수집 테스트
echo -e "\n${BLUE}9. 전체 AWS 데이터 수집 시작...${NC}"
COLLECT_ALL_RESPONSE=$(curl -s -X POST "${BACKEND_SERVICE_URL}/api/aws-data/collect-all/${ACCOUNT_ID}" \
    -H "Authorization: Bearer $ACCESS_TOKEN")

echo "전체 데이터 수집 응답: $COLLECT_ALL_RESPONSE"

echo -e "\n${GREEN}=== AWS API 테스트 완료 ===${NC}"
echo -e "${BLUE}참고: 데이터 수집은 비동기로 처리되므로 완료까지 몇 분이 소요될 수 있습니다.${NC}"
echo -e "${BLUE}로그를 확인하여 수집 진행 상황을 모니터링하세요.${NC}"
