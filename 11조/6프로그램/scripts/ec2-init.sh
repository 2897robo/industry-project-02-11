#!/bin/bash

echo "🚀 CostWise EC2 초기 설정 스크립트"
echo "======================================"

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 1. 시스템 업데이트
echo -e "${YELLOW}시스템 패키지 업데이트 중...${NC}"
sudo yum update -y

# 2. Docker 설치
echo -e "${YELLOW}Docker 설치 중...${NC}"
sudo amazon-linux-extras install docker -y
sudo service docker start
sudo usermod -a -G docker ec2-user
sudo chkconfig docker on

# 3. Docker Compose 설치
echo -e "${YELLOW}Docker Compose 설치 중...${NC}"
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 4. Git 설치
echo -e "${YELLOW}Git 설치 중...${NC}"
sudo yum install git -y

# 5. AWS CLI 설치 (이미 설치되어 있을 수 있음)
echo -e "${YELLOW}AWS CLI 확인 중...${NC}"
if ! command -v aws &> /dev/null; then
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip awscliv2.zip
    sudo ./aws/install
    rm -rf awscliv2.zip aws/
fi

# 6. 프로젝트 디렉토리 생성 및 클론
echo -e "${YELLOW}프로젝트 클론 중...${NC}"
cd /home/ec2-user
git clone https://github.com/industry-project-02-11/industry-project-02-11.git app
cd app

# 7. 환경 변수 파일 생성 안내
echo -e "${YELLOW}환경 변수 파일 생성이 필요합니다!${NC}"
echo ""
echo "다음 명령어로 환경 변수 파일을 생성하세요:"
echo "  cd /home/ec2-user/app/11조/6프로그램"
echo "  cp .env.prod.example .env"
echo "  nano .env"
echo ""
echo "필수 환경 변수:"
echo "  - DB_URL: RDS PostgreSQL 엔드포인트"
echo "  - DB_USERNAME, DB_PASSWORD: 데이터베이스 인증 정보"
echo "  - REDIS_HOST: ElastiCache Redis 엔드포인트"
echo "  - JWT_SECRET: JWT 시크릿 키 (32자 이상)"
echo "  - AES_KEY: AES 암호화 키 (32자)"
echo "  - ECR_REGISTRY: ECR 레지스트리 URL"

# 8. SSL 인증서 디렉토리 생성
echo -e "${YELLOW}SSL 인증서 디렉토리 생성 중...${NC}"
mkdir -p /home/ec2-user/app/11조/6프로그램/nginx/ssl
mkdir -p /home/ec2-user/app/11조/6프로그램/certbot/www

# 9. 권한 설정
echo -e "${YELLOW}권한 설정 중...${NC}"
sudo chown -R ec2-user:ec2-user /home/ec2-user/app

echo ""
echo -e "${GREEN}✅ EC2 초기 설정이 완료되었습니다!${NC}"
echo ""
echo "다음 단계:"
echo "1. 환경 변수 파일 설정: nano /home/ec2-user/app/11조/6프로그램/.env"
echo "2. ECR 로그인: aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin [ECR_REGISTRY]"
echo "3. SSL 인증서 설정 (Let's Encrypt 사용 권장)"
echo "4. 배포 실행: cd /home/ec2-user/app/11조/6프로그램 && ./deploy-prod.sh"
echo ""
echo -e "${YELLOW}재부팅 후 docker 그룹이 적용됩니다. 재부팅하시겠습니까? (y/n)${NC}"
read -r response
if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
    sudo reboot
fi