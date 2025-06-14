#!/bin/bash

echo "🚀 Team11 AWS Cost Optimization Service 시작 스크립트"
echo "=================================================="

# 현재 디렉토리 저장
PROJECT_ROOT=$(pwd)

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 함수: 성공 메시지
success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# 함수: 에러 메시지
error() {
    echo -e "${RED}❌ $1${NC}"
}

# 함수: 정보 메시지
info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# 1. Docker 및 Docker Compose 확인
info "Docker 환경 확인 중..."
if ! command -v docker &> /dev/null; then
    error "Docker가 설치되어 있지 않습니다!"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    error "Docker Compose가 설치되어 있지 않습니다!"
    exit 1
fi
success "Docker 환경 확인 완료"

# 2. 기존 컨테이너 정리
info "기존 컨테이너 정리 중..."
docker-compose down
success "기존 컨테이너 정리 완료"

# 3. Gradle Wrapper 복구
info "Gradle Wrapper 파일 복구 중..."
for service in eureka-discovery-service gateway-service auth-service user-service backend; do
    SERVICE_DIR="$PROJECT_ROOT/apps/$service"
    if [ -d "$SERVICE_DIR" ]; then
        cd "$SERVICE_DIR"
        
        # gradle wrapper 재생성
        if [ ! -f "gradlew" ]; then
            info "$service: Gradle Wrapper 생성 중..."
            gradle wrapper --gradle-version=8.13 || {
                error "$service: Gradle Wrapper 생성 실패"
                # 대안: 다른 서비스에서 복사
                if [ -f "$PROJECT_ROOT/apps/backend/gradlew" ]; then
                    cp -r "$PROJECT_ROOT/apps/backend/gradle" "$SERVICE_DIR/"
                    cp "$PROJECT_ROOT/apps/backend/gradlew" "$SERVICE_DIR/"
                    cp "$PROJECT_ROOT/apps/backend/gradlew.bat" "$SERVICE_DIR/"
                    chmod +x "$SERVICE_DIR/gradlew"
                fi
            }
        fi
        
        # 실행 권한 부여
        chmod +x gradlew 2>/dev/null || true
    fi
done
cd "$PROJECT_ROOT"
success "Gradle Wrapper 복구 완료"

# 4. 각 서비스 빌드
info "서비스 빌드 시작..."
BUILD_FAILED=0

for service in eureka-discovery-service gateway-service auth-service user-service backend; do
    SERVICE_DIR="$PROJECT_ROOT/apps/$service"
    if [ -d "$SERVICE_DIR" ]; then
        info "Building $service..."
        cd "$SERVICE_DIR"
        
        # Gradle 빌드 실행
        if [ -f "gradlew" ]; then
            ./gradlew clean build -x test || {
                error "$service 빌드 실패!"
                BUILD_FAILED=1
            }
        else
            error "$service: gradlew 파일을 찾을 수 없습니다!"
            BUILD_FAILED=1
        fi
    fi
done

cd "$PROJECT_ROOT"

if [ $BUILD_FAILED -eq 1 ]; then
    error "일부 서비스 빌드가 실패했습니다. 계속하시겠습니까? (y/n)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

success "서비스 빌드 완료"

# 5. Docker Compose로 서비스 실행
info "Docker Compose로 서비스 시작 중..."

# 기본 인프라 (PostgreSQL, Redis) 먼저 시작
docker-compose up -d
sleep 10  # 데이터베이스 초기화 대기

# 전체 서비스 시작
docker-compose -f docker-compose.yml -f docker-compose.services.yml up -d

# 6. 서비스 상태 확인
info "서비스 상태 확인 중... (30초 대기)"
sleep 30

echo ""
echo "📊 실행 중인 컨테이너:"
docker-compose -f docker-compose.yml -f docker-compose.services.yml ps

echo ""
echo "🔗 서비스 접속 정보:"
echo "  - Eureka Dashboard: http://localhost:8761"
echo "  - API Gateway: http://localhost:8000"
echo "  - Frontend: http://localhost:5173"
echo "  - PostgreSQL: localhost:5432"
echo "  - Redis: localhost:6379"

echo ""
echo "📝 로그 확인:"
echo "  전체 로그: docker-compose -f docker-compose.yml -f docker-compose.services.yml logs -f"
echo "  특정 서비스: docker-compose logs -f [service-name]"

echo ""
success "모든 서비스가 시작되었습니다! 🎉"

# 7. 헬스체크 (옵션)
echo ""
info "서비스 헬스체크를 수행하시겠습니까? (y/n)"
read -r response
if [[ "$response" =~ ^[Yy]$ ]]; then
    info "Eureka 서비스 등록 확인 중..."
    sleep 10
    curl -s http://localhost:8761/eureka/apps | grep -q "application" && success "Eureka 정상 작동 중" || error "Eureka 응답 없음"
fi
