# 최종 배포 체크리스트

## 🚀 현재 상태
- ✅ RDS PostgreSQL 생성 완료
- ✅ ElastiCache Redis 생성 완료  
- ✅ EC2 인스턴스 생성 완료 (자동 배포 진행 중)
- ⏳ Docker 이미지 Pull 및 서비스 시작 중 (5-10분 소요)

## 📍 서버 정보
- **EC2 Public IP**: 13.125.205.25
- **인스턴스 ID**: i-01327ac79059dc1a6
- **도메인**: costwise.site

## 1️⃣ 가비아 DNS 설정 (지금 바로 하세요!)

### 가비아 관리 페이지 접속
1. https://www.gabia.com 로그인
2. My 가비아 → 서비스 관리 → 도메인
3. `costwise.site` 찾기 → DNS 설정 클릭

### DNS 레코드 설정
**기존 레코드 모두 삭제 후 다음 레코드 추가:**

| 타입 | 호스트 | 값 | TTL |
|------|--------|-----|-----|
| A | @ | 13.125.205.25 | 300 |
| A | api | 13.125.205.25 | 300 |
| A | www | 13.125.205.25 | 300 |

## 2️⃣ 배포 상태 확인 (5분 후)

### 터미널에서 확인
```bash
# API Gateway 헬스체크
curl http://13.125.205.25:8000/actuator/health

# Nginx 확인
curl http://13.125.205.25
```

### 브라우저에서 확인
- http://13.125.205.25:8761 - Eureka Dashboard
- http://13.125.205.25:8000 - API Gateway

## 3️⃣ 프론트엔드 설정 (Vercel)

### 3.1 프론트엔드 환경변수 업데이트
Vercel 대시보드에서:
1. 프로젝트 선택
2. Settings → Environment Variables
3. 다음 변수 추가/수정:
```
VITE_API_URL=https://api.costwise.site
```

### 3.2 도메인 설정
1. Settings → Domains
2. `costwise.site` 추가
3. 가비아에서 CNAME 레코드 추가:
   - 타입: CNAME
   - 호스트: @
   - 값: cname.vercel-dns.com
   - TTL: 300

## 4️⃣ HTTPS 설정 (DNS 전파 후 - 약 30분)

### EC2에서 Let's Encrypt SSL 인증서 설치
```bash
# SSH 접속 (작동하지 않으면 AWS Session Manager 사용)
ssh -i ../../costwise-key.pem ubuntu@13.125.205.25

# SSL 인증서 발급
sudo certbot --nginx -d costwise.site -d api.costwise.site -d www.costwise.site --non-interactive --agree-tos -m your-email@example.com
```

## 5️⃣ 최종 확인

### DNS 전파 확인
```bash
# DNS 조회
nslookup api.costwise.site
nslookup costwise.site

# 또는
dig api.costwise.site
dig costwise.site
```

### 서비스 작동 확인
- https://costwise.site - 프론트엔드 (Vercel)
- https://api.costwise.site - 백엔드 API
- https://api.costwise.site/swagger-ui.html - API 문서

## 🛠 트러블슈팅

### 서비스가 시작되지 않은 경우
AWS 콘솔에서:
1. EC2 → 인스턴스 → i-01327ac79059dc1a6 선택
2. 작업 → 모니터링 및 문제 해결 → 시스템 로그 가져오기
3. 로그에서 오류 확인

### Docker 이미지 Pull 실패
- ECR 권한 확인
- IAM 역할이 제대로 연결되었는지 확인

### 데이터베이스 연결 실패
- RDS 보안 그룹 확인
- EC2 보안 그룹이 RDS 보안 그룹에 허용되었는지 확인

## 📱 모니터링

### CloudWatch 로그
AWS 콘솔에서 CloudWatch → 로그 그룹에서 확인

### 서비스 로그 (SSH 접속 후)
```bash
cd /home/ubuntu/industry-project-02-11/11조/6프로그램
docker-compose -f docker-compose.prod.yml logs -f
```

## ✅ 완료!

모든 설정이 완료되면 https://costwise.site 에서 서비스를 사용할 수 있습니다!