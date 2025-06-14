#!/bin/bash

echo "🚀 Team11 프로덕션 배포 스크립트"
echo "================================"

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 1. 환경변수 파일 확인
if [ ! -f .env ]; then
    echo -e "${RED}❌ .env 파일이 없습니다!${NC}"
    echo "cp .env.example .env 명령어로 생성 후 수정하세요."
    exit 1
fi

# 2. 기존 컨테이너 정지
echo -e "${YELLOW}기존 컨테이너 정지 중...${NC}"
docker-compose -f docker-compose.prod.yml down

# 3. 최신 이미지 풀
echo -e "${YELLOW}최신 이미지 다운로드 중...${NC}"
docker-compose -f docker-compose.prod.yml pull

# 4. 컨테이너 시작
echo -e "${YELLOW}컨테이너 시작 중...${NC}"
docker-compose -f docker-compose.prod.yml up -d

# 5. 헬스체크 대기
echo -e "${YELLOW}서비스 시작 대기 중... (60초)${NC}"
sleep 60

# 6. 서비스 상태 확인
echo -e "${YELLOW}서비스 상태 확인 중...${NC}"
docker-compose -f docker-compose.prod.yml ps

# 7. Eureka 헬스체크
echo -e "${YELLOW}Eureka 헬스체크...${NC}"
curl -s http://localhost:8761/actuator/health | grep -q "UP" && \
    echo -e "${GREEN}✅ Eureka 정상 작동 중${NC}" || \
    echo -e "${RED}❌ Eureka 응답 없음${NC}"

# 8. Gateway 헬스체크
echo -e "${YELLOW}Gateway 헬스체크...${NC}"
curl -s http://localhost/actuator/health | grep -q "UP" && \
    echo -e "${GREEN}✅ Gateway 정상 작동 중${NC}" || \
    echo -e "${RED}❌ Gateway 응답 없음${NC}"

echo ""
echo -e "${GREEN}✅ 배포가 완료되었습니다!${NC}"
echo ""
echo "모니터링 명령어:"
echo "  - 로그 확인: docker-compose -f docker-compose.prod.yml logs -f"
echo "  - 상태 확인: docker-compose -f docker-compose.prod.yml ps"
echo ""
echo "서비스 URL:"
echo "  - API Gateway: http://your-domain.com"
echo "  - Eureka Dashboard: http://your-domain.com:8761"
