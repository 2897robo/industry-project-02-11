#!/bin/bash

# 색상 코드 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== AES 키 문제 해결 스크립트 ===${NC}\n"

# 1. 현재 설정 백업
echo -e "${BLUE}1. 현재 설정 백업 중...${NC}"
cp 11조/6프로그램/apps/backend/src/main/resources/application-dev.yml \
   11조/6프로그램/apps/backend/src/main/resources/application-dev.yml.backup

# 2. 올바른 AES 키 생성 (32자)
NEW_AES_KEY="aes256key1234567890abcdefghijklm"  # 정확히 32자
echo -e "${GREEN}✓ 새로운 AES 키 생성: ${NEW_AES_KEY}${NC}"

# 3. application-dev.yml 수정
echo -e "\n${BLUE}2. application-dev.yml 파일 수정 중...${NC}"
sed -i '' "s/key: keyb0b5a4b9a31e5ba2aee85ee1d6c255gg/key: ${NEW_AES_KEY}/" \
    11조/6프로그램/apps/backend/src/main/resources/application-dev.yml

echo -e "${GREEN}✓ 설정 파일이 수정되었습니다.${NC}"

# 4. 수정된 내용 확인
echo -e "\n${BLUE}3. 수정된 AES 설정:${NC}"
grep -A 2 "aes:" 11조/6프로그램/apps/backend/src/main/resources/application-dev.yml

echo -e "\n${YELLOW}⚠️  주의사항:${NC}"
echo "1. Backend 서비스를 재시작해야 변경사항이 적용됩니다."
echo "2. IntelliJ에서 실행 중이라면 중지 후 다시 실행하세요."
echo "3. 또는 다음 명령어로 Gradle로 실행할 수 있습니다:"
echo -e "${GREEN}   cd 11조/6프로그램/apps/backend && ./gradlew bootRun${NC}"
