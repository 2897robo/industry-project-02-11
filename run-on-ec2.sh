#!/bin/bash

# EC2에 SSH로 접속해서 배포 실행하는 스크립트
# 로컬에서 실행합니다.

# EC2 정보 (실제 값으로 변경하세요)
EC2_HOST="ubuntu@3.35.52.87"        # EC2 Public IP
PEM_KEY_PATH="./costwise-key.pem"  # PEM 키 파일 경로

echo "🚀 EC2에 접속하여 배포 시작..."

# PEM 파일 권한 설정
chmod 400 $PEM_KEY_PATH

# SSH로 EC2에 접속하여 명령 실행
ssh -i $PEM_KEY_PATH $EC2_HOST << 'ENDSSH'
echo "📦 EC2에서 작업 시작..."

# 프로젝트 디렉토리로 이동
cd ~/industry-project-02-11

# Git pull로 최신 코드 가져오기
echo "📥 최신 코드 가져오기..."
git pull

# 배포 스크립트 실행 권한 부여
chmod +x 11조/6프로그램/emergency-deploy.sh
chmod +x 11조/6프로그램/create-dockerfiles.sh

# Dockerfile 생성
echo "📝 Dockerfile 생성..."
cd 11조/6프로그램
./create-dockerfiles.sh

# 긴급 배포 실행
echo "🚀 긴급 배포 시작..."
./emergency-deploy.sh

echo "✅ 배포 작업 완료!"
ENDSSH

echo "🎉 모든 작업이 완료되었습니다!"
