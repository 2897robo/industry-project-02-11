# 🚀 CostWise 배포 현황

## 📊 현재 상태 (2025-06-16 11:50 KST)

### ✅ 완료된 작업
1. **AWS 리소스 생성 완료**
   - RDS PostgreSQL: `team11-cloud-cost-db.cvccass28uoc.ap-northeast-2.rds.amazonaws.com`
   - ElastiCache Redis: `team11-cloud-cost-redis.jlfbxu.0001.apn2.cache.amazonaws.com`
   - EC2 인스턴스: `i-0a7668e9f07e26f83` (IP: `13.125.234.248`)
   
2. **Nginx 실행 중**
   - 80 포트로 접속 시 502 응답 (백엔드 대기 중)

### ⏳ 진행 중인 작업
- Docker 이미지 Pull 및 컨테이너 시작 (예상 소요 시간: 10-15분)

## 🔍 상태 확인 방법

### 1. AWS 콘솔에서 확인
1. [AWS EC2 콘솔](https://ap-northeast-2.console.aws.amazon.com/ec2/home?region=ap-northeast-2#Instances:)
2. 인스턴스 `i-0a7668e9f07e26f83` 선택
3. **작업** → **모니터링 및 문제 해결** → **시스템 로그 가져오기**
4. User Data 스크립트 실행 로그 확인

### 2. AWS Session Manager로 접속
1. EC2 콘솔에서 인스턴스 선택
2. **연결** 버튼 클릭
3. **Session Manager** 탭 선택
4. **연결** 클릭
5. 다음 명령어로 상태 확인:
```bash
# 로그 확인
sudo tail -f /var/log/user-data.log

# Docker 상태 확인
cd /home/ubuntu/industry-project-02-11/11조/6프로그램
sudo docker-compose -f docker-compose.prod.yml ps

# 서비스 로그 확인
sudo docker-compose -f docker-compose.prod.yml logs -f
```

## 🌐 가비아 DNS 설정 (아직 안 하셨다면!)

1. https://www.gabia.com 로그인
2. **My 가비아** → **서비스 관리** → **도메인**
3. `costwise.site` → **DNS 설정**
4. 기존 레코드 삭제 후 추가:

| 타입 | 호스트 | 값 | TTL |
|------|--------|-----|-----|
| A | @ | 13.125.234.248 | 300 |
| A | api | 13.125.234.248 | 300 |
| A | www | 13.125.234.248 | 300 |

## 📱 프론트엔드 설정 (Vercel)

### 환경변수 업데이트
1. [Vercel Dashboard](https://vercel.com/dashboard)
2. 프로젝트 선택
3. **Settings** → **Environment Variables**
4. 추가/수정:
```
VITE_API_URL=https://api.costwise.site
```
5. **Redeploy** 클릭

### 도메인 연결
1. **Settings** → **Domains**
2. `costwise.site` 추가
3. Vercel이 제공하는 DNS 설정을 가비아에 추가

## ⏰ 예상 완료 시간
- 백엔드 서비스 시작: 10-15분
- DNS 전파: 10-30분
- SSL 인증서 설정: DNS 전파 후 가능

## 🔧 트러블슈팅

### 서비스가 시작되지 않는 경우
1. ECR 이미지가 제대로 빌드되었는지 확인
2. IAM 역할이 제대로 연결되었는지 확인
3. RDS/Redis 보안 그룹 설정 확인

### 502 에러가 계속되는 경우
Session Manager로 접속해서:
```bash
# Docker 재시작
cd /home/ubuntu/industry-project-02-11/11조/6프로그램
sudo docker-compose -f docker-compose.prod.yml down
sudo docker-compose -f docker-compose.prod.yml up -d

# 로그 확인
sudo docker-compose -f docker-compose.prod.yml logs -f gateway-service
```

## 📞 지원
문제가 지속되면 다음 정보와 함께 문의:
- 인스턴스 ID: `i-0a7668e9f07e26f83`
- 시스템 로그 스크린샷
- Docker 로그 내용