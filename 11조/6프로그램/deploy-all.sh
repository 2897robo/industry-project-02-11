#!/bin/bash

# Team 11 Cloud Cost Platform - 전체 배포 마스터 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
NC='\033[0m'

echo -e "${MAGENTA}============================================${NC}"
echo -e "${MAGENTA}  Team 11 Cloud Cost Platform 전체 배포${NC}"
echo -e "${MAGENTA}============================================${NC}"

# 1단계: IAM 역할 설정
echo -e "${BLUE}[Step 1/5] IAM 역할 설정${NC}"
read -p "IAM 역할을 설정하시겠습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    chmod +x setup-iam-roles.sh
    ./setup-iam-roles.sh
else
    echo -e "${YELLOW}IAM 역할 설정을 건너뜁니다.${NC}"
fi

# 2단계: ECR 리포지토리 확인
echo -e "${BLUE}[Step 2/5] ECR 리포지토리 확인${NC}"
chmod +x setup-ecr-repositories.sh
./setup-ecr-repositories.sh

# 3단계: Docker 이미지 빌드 및 푸시
echo -e "${BLUE}[Step 3/5] Docker 이미지 빌드 및 ECR 푸시${NC}"
read -p "Docker 이미지를 빌드하고 ECR에 푸시하시겠습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    chmod +x build-and-push-to-ecr.sh
    ./build-and-push-to-ecr.sh
else
    echo -e "${YELLOW}Docker 이미지 빌드를 건너뜁니다.${NC}"
fi

# 4단계: AWS 인프라 구축
echo -e "${BLUE}[Step 4/5] AWS 인프라 구축 (VPC, RDS, Redis, EC2)${NC}"
echo -e "${RED}⚠️  주의: 이 단계는 AWS 리소스를 생성하며 비용이 발생합니다!${NC}"
read -p "AWS 인프라를 구축하시겠습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    chmod +x aws-deploy-complete.sh
    ./aws-deploy-complete.sh
else
    echo -e "${YELLOW}AWS 인프라 구축을 건너뜁니다.${NC}"
fi

# 5단계: 애플리케이션 배포
echo -e "${BLUE}[Step 5/5] EC2에 애플리케이션 배포${NC}"
if [ -f "deploy-to-ec2.sh" ]; then
    read -p "EC2에 애플리케이션을 배포하시겠습니까? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        chmod +x deploy-to-ec2.sh
        ./deploy-to-ec2.sh
    else
        echo -e "${YELLOW}애플리케이션 배포를 건너뜁니다.${NC}"
    fi
else
    echo -e "${YELLOW}인프라가 아직 구축되지 않았습니다. Step 4를 먼저 실행하세요.${NC}"
fi

echo -e "${MAGENTA}============================================${NC}"
echo -e "${GREEN}✅ 배포 프로세스가 완료되었습니다!${NC}"
echo -e "${MAGENTA}============================================${NC}"

# 배포 정보 출력
if [ -f ".env.production" ]; then
    echo -e "${BLUE}배포 정보:${NC}"
    grep "PUBLIC_IP=" .env.production 2>/dev/null || true
    echo ""
    echo -e "서비스 확인:"
    echo -e "  - API Gateway: http://[EC2-IP]:8000"
    echo -e "  - Eureka Dashboard: http://[EC2-IP]:8761"
fi
