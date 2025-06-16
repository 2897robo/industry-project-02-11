#!/bin/bash

# 로컬에서 빌드하고 이미지를 EC2로 전송하는 스크립트
# 로컬에서 실행합니다.

set -e

echo "🔨 로컬에서 서비스 빌드 시작..."

# EC2 정보
EC2_HOST="ubuntu@52.79.119.60"  # EC2 IP 주소로 변경하세요
EC2_KEY="~/costwise-key.pem"    # PEM 키 경로

# 프로젝트 디렉토리로 이동
cd 11조/6프로그램

# 각 서비스 빌드
services=("eureka-discovery-service" "gateway-service" "auth-service" "user-service" "backend")

for service in "${services[@]}"; do
    echo "  📌 $service 빌드 중..."
    cd apps/$service
    
    # Gradle 빌드
    chmod +x gradlew
    ./gradlew clean bootJar
    
    # Docker 이미지 빌드
    docker build -t team11-${service}:latest .
    
    # 이미지를 tar 파일로 저장
    docker save team11-${service}:latest | gzip > team11-${service}.tar.gz
    
    # EC2로 전송
    echo "  📤 $service 이미지를 EC2로 전송 중..."
    scp -i $EC2_KEY team11-${service}.tar.gz $EC2_HOST:~/
    
    # EC2에서 이미지 로드
    ssh -i $EC2_KEY $EC2_HOST "sudo docker load < ~/team11-${service}.tar.gz"
    
    # 임시 파일 삭제
    rm team11-${service}.tar.gz
    ssh -i $EC2_KEY $EC2_HOST "rm ~/team11-${service}.tar.gz"
    
    cd ../..
done

echo "✅ 모든 이미지 전송 완료!"
echo "📝 EC2에서 emergency-deploy.sh 실행하세요"
