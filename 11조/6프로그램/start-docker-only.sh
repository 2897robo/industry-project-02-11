#!/bin/bash

echo "🐳 Docker로 전체 서비스 실행 (빌드 없이)"
echo "========================================"

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. 기존 컨테이너 정리
echo -e "${YELLOW}기존 컨테이너 정리 중...${NC}"
docker-compose down

# 2. PostgreSQL과 Redis 먼저 시작
echo -e "${YELLOW}데이터베이스 서비스 시작 중...${NC}"
docker-compose up -d
sleep 10

# 3. 모든 서비스 시작 (이미 빌드된 이미지 사용)
echo -e "${YELLOW}애플리케이션 서비스 시작 중...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.services.yml up -d

# 4. 상태 확인
echo ""
echo -e "${GREEN}✅ 서비스 시작 완료!${NC}"
echo ""
echo "📊 실행 중인 서비스:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "🔗 접속 정보:"
echo "  - Eureka: http://localhost:8761"
echo "  - API Gateway: http://localhost:8000"
echo "  - Frontend: http://localhost:5173"

echo ""
echo "📝 로그 보기:"
echo "  docker-compose logs -f [서비스명]"
