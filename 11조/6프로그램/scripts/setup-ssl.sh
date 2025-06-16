#!/bin/bash

echo "🔐 CostWise SSL 인증서 설정 (Let's Encrypt)"
echo "==========================================="

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 도메인 설정
DOMAIN="costwise.site"
DOMAINS="-d costwise.site -d www.costwise.site -d api.costwise.site"
EMAIL="admin@costwise.site"  # 실제 이메일로 변경 필요

# 1. Certbot 설치
echo -e "${YELLOW}Certbot 설치 중...${NC}"
sudo yum install -y certbot

# 2. nginx 컨테이너 중지 (포트 충돌 방지)
echo -e "${YELLOW}Nginx 컨테이너 일시 중지...${NC}"
cd /home/ec2-user/app/11조/6프로그램
docker-compose -f docker-compose.prod.yml stop nginx

# 3. 인증서 발급
echo -e "${YELLOW}SSL 인증서 발급 중...${NC}"
sudo certbot certonly \
    --standalone \
    --preferred-challenges http \
    --email $EMAIL \
    --agree-tos \
    --no-eff-email \
    --force-renewal \
    $DOMAINS

# 4. 인증서 복사
echo -e "${YELLOW}인증서 복사 중...${NC}"
sudo cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem /home/ec2-user/app/11조/6프로그램/nginx/ssl/
sudo cp /etc/letsencrypt/live/$DOMAIN/privkey.pem /home/ec2-user/app/11조/6프로그램/nginx/ssl/
sudo chown ec2-user:ec2-user /home/ec2-user/app/11조/6프로그램/nginx/ssl/*

# 5. nginx 재시작
echo -e "${YELLOW}Nginx 재시작 중...${NC}"
docker-compose -f docker-compose.prod.yml start nginx

# 6. 자동 갱신 설정
echo -e "${YELLOW}자동 갱신 Cron 설정 중...${NC}"
(crontab -l 2>/dev/null; echo "0 0,12 * * * /usr/bin/certbot renew --quiet --post-hook 'cd /home/ec2-user/app/11조/6프로그램 && docker-compose -f docker-compose.prod.yml restart nginx'") | crontab -

echo ""
echo -e "${GREEN}✅ SSL 인증서 설정이 완료되었습니다!${NC}"
echo ""
echo "인증서 위치:"
echo "  - Fullchain: /home/ec2-user/app/11조/6프로그램/nginx/ssl/fullchain.pem"
echo "  - Private Key: /home/ec2-user/app/11조/6프로그램/nginx/ssl/privkey.pem"
echo ""
echo "자동 갱신이 매일 00:00와 12:00에 실행됩니다."