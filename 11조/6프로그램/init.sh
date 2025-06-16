#!/bin/bash

# 초기 설정 스크립트

echo "Team 11 Cloud Cost Platform 배포 초기화"
echo "========================================"

# 모든 스크립트에 실행 권한 부여
echo "스크립트 실행 권한 설정 중..."
chmod +x deploy-all.sh
chmod +x setup-iam-roles.sh
chmod +x setup-ecr-repositories.sh
chmod +x build-and-push-to-ecr.sh
chmod +x aws-deploy-complete.sh
chmod +x manage-deployment.sh
chmod +x deploy-prod.sh
chmod +x build-and-push.sh

echo "✅ 실행 권한 설정 완료"
echo ""

# AWS CLI 확인
echo "AWS CLI 확인 중..."
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI가 설치되어 있지 않습니다."
    echo "다음 명령으로 설치하세요:"
    echo "  curl \"https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip\" -o \"awscliv2.zip\""
    echo "  unzip awscliv2.zip"
    echo "  sudo ./aws/install"
    exit 1
fi

# AWS 자격 증명 확인
echo "AWS 자격 증명 확인 중..."
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS 자격 증명이 설정되지 않았습니다."
    echo "다음 명령으로 설정하세요:"
    echo "  aws configure"
    exit 1
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=$(aws configure get region)

echo "✅ AWS 설정 확인 완료"
echo "  - 계정 ID: $ACCOUNT_ID"
echo "  - 리전: $REGION"
echo ""

# Docker 확인
echo "Docker 확인 중..."
if ! command -v docker &> /dev/null; then
    echo "⚠️  Docker가 설치되어 있지 않습니다."
    echo "로컬에서 이미지를 빌드하려면 Docker가 필요합니다."
fi

# 필수 도구 확인
echo "필수 도구 확인 중..."
tools=("git" "curl" "jq")
missing_tools=()

for tool in "${tools[@]}"; do
    if ! command -v $tool &> /dev/null; then
        missing_tools+=($tool)
    fi
done

if [ ${#missing_tools[@]} -ne 0 ]; then
    echo "⚠️  다음 도구가 필요합니다: ${missing_tools[*]}"
    echo "설치 명령: sudo apt-get install -y ${missing_tools[*]}"
fi

echo ""
echo "========================================"
echo "✅ 초기화 완료!"
echo ""
echo "다음 명령으로 배포를 시작하세요:"
echo "  ./deploy-all.sh"
echo ""
echo "또는 개별 단계 실행:"
echo "  1. ./setup-iam-roles.sh      # IAM 역할 설정"
echo "  2. ./setup-ecr-repositories.sh # ECR 리포지토리 생성"
echo "  3. ./build-and-push-to-ecr.sh # Docker 이미지 빌드 및 푸시"
echo "  4. ./aws-deploy-complete.sh   # AWS 인프라 구축"
echo "  5. ./deploy-to-ec2.sh        # 애플리케이션 배포"
echo ""
echo "배포 후 관리:"
echo "  ./manage-deployment.sh       # 관리 도구"
echo "========================================"
