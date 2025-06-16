#!/bin/bash
set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}시스템 업데이트 중...${NC}"
sudo apt update && sudo apt upgrade -y

echo -e "${YELLOW}필수 패키지 설치 중...${NC}"
sudo apt install -y curl wget git vim htop unzip nginx certbot python3-certbot-nginx

echo -e "${YELLOW}Docker 설치 중...${NC}"
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker ubuntu
    rm get-docker.sh
fi

echo -e "${YELLOW}Docker Compose 설치 중...${NC}"
if ! command -v docker-compose &> /dev/null; then
    sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi

echo -e "${YELLOW}AWS CLI 설치 중...${NC}"
if ! command -v aws &> /dev/null; then
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip -q awscliv2.zip
    sudo ./aws/install
    rm -rf awscliv2.zip aws
fi

echo -e "${GREEN}✓ 시스템 준비 완료${NC}"
