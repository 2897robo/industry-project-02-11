# 📘 Cloud Cost Optimization API 명세서 v2.0

## 🔐 인증 관련 API

### 회원가입
```http
POST /users
Content-Type: application/json

{
  "uid": "user123",
  "password": "password123",
  "name": "홍길동"
}
```

### 로그인
```http
GET /users/login?uid=user123&password=password123
```

### 사용자 정보 조회
```http
GET /users
Authorization: Bearer {token}
```

### 중복 확인
```http
GET /users/check?uid=user123
```

---

## 🔑 AWS 계정 관리 API

### AWS 계정 등록
```http
POST /api/aws-accounts
Authorization: Bearer {token}
Content-Type: application/json

{
  "accountAlias": "Production Account",
  "awsAccountId": "123456789012",
  "accessKeyId": "AKIA...",
  "secretAccessKey": "wJalrXUtnFEMI...",
  "region": "ap-northeast-2"
}
```

### 내 AWS 계정 목록 조회
```http
GET /api/aws-accounts
Authorization: Bearer {token}
```

### AWS 계정 상세 조회
```http
GET /api/aws-accounts/{accountId}
Authorization: Bearer {token}
```

### AWS 계정 수정
```http
PUT /api/aws-accounts/{accountId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "accountAlias": "Updated Name",
  "region": "us-east-1",
  "accessKeyId": "AKIA...",  // optional
  "secretAccessKey": "..."     // optional
}
```

### AWS 계정 비활성화
```http
DELETE /api/aws-accounts/{accountId}
Authorization: Bearer {token}
```

---

## 📊 리소스 관리 API

### 내 리소스 목록 조회
```http
GET /api/resources
Authorization: Bearer {token}
```

### 서비스 타입별 리소스 조회
```http
GET /api/resources/by-service-type/{serviceType}
Authorization: Bearer {token}

# serviceType: EC2, RDS, S3, Lambda, EBS, CloudFront, DynamoDB, ElastiCache, ELB, Route53
```

### 유휴 리소스만 조회
```http
GET /api/resources/idle
Authorization: Bearer {token}
```

### 리소스 상세 조회
```http
GET /api/resources/{id}
Authorization: Bearer {token}
```

---

## 💰 비용 이력 API

### 비용 이력 조회
```http
GET /api/cost-history?startDate=2025-01-01&endDate=2025-01-31
Authorization: Bearer {token}
```

### AWS 계정별 비용 이력
```http
GET /api/cost-history/aws-account/{awsAccountId}?startDate=2025-01-01&endDate=2025-01-31
Authorization: Bearer {token}
```

### 서비스별 비용 요약
```http
GET /api/cost-history/service-summary?startDate=2025-01-01&endDate=2025-01-31
Authorization: Bearer {token}
```

### 일별 비용 추이
```http
GET /api/cost-history/daily-trend?startDate=2025-01-01&endDate=2025-01-31
Authorization: Bearer {token}
```

### 현재 월 비용 요약
```http
GET /api/cost-history/current-month
Authorization: Bearer {token}
```

### 월별 비용 추이
```http
GET /api/cost-history/monthly-trend?months=6
Authorization: Bearer {token}
```

---

## 🚀 데이터 수집 API

### 리소스 수집 (수동)
```http
POST /api/aws-data/collect-resources/{awsAccountId}
Authorization: Bearer {token}
```

### 비용 데이터 수집 (수동)
```http
POST /api/aws-data/collect-costs/{awsAccountId}?startDate=2025-01-01&endDate=2025-01-31
Authorization: Bearer {token}
```

### 리소스 메트릭 업데이트 (수동)
```http
POST /api/aws-data/update-metrics/{awsAccountId}
Authorization: Bearer {token}
```

### 전체 데이터 수집
```http
POST /api/aws-data/collect-all/{awsAccountId}
Authorization: Bearer {token}
```

---

## 💡 추천 관리 API

### 내 추천 목록 조회
```http
GET /api/recommendations?status=pending
Authorization: Bearer {token}

# status: pending(기본값), accepted, ignored, all
```

### 추천 생성 (전체 리소스 분석)
```http
POST /api/recommendations/generate
Authorization: Bearer {token}
```

### 특정 리소스 추천 재생성
```http
POST /api/recommendations/generate/resource/{resourceId}
Authorization: Bearer {token}
```

### 추천 요약 정보
```http
GET /api/recommendations/summary
Authorization: Bearer {token}

Response:
{
  "totalRecommendations": 15,
  "totalExpectedSaving": 1234.56,
  "currency": "USD",
  "monthlyProjectedSaving": 37036.80
}
```

### 추천 상세 조회
```http
GET /api/recommendations/{id}
Authorization: Bearer {token}
```

### 추천 상태 업데이트
```http
PUT /api/recommendations/{id}/status
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": "accepted"  // or "ignored"
}
```

### 리소스별 추천 조회
```http
GET /api/recommendations/resource/{resourceId}
Authorization: Bearer {token}
```

---

## ⚙️ 설정 관리 API

### 내 설정 조회
```http
GET /api/configs
Authorization: Bearer {token}
```

### 설정 생성/업데이트
```http
POST /api/configs
Authorization: Bearer {token}
Content-Type: application/json

{
  "idleThreshold": 20.0,
  "budgetLimit": 1000
}
```

---

## 🔔 알림 관리 API

### 알림 목록 조회
```http
GET /api/alerts
Authorization: Bearer {token}
```

### 알림 생성
```http
POST /api/alerts
Authorization: Bearer {token}
Content-Type: application/json

{
  "alertType": "BUDGET_EXCEEDED",
  "channel": "EMAIL",
  "message": "월 예산을 초과했습니다."
}
```

---

## 📈 대시보드 API

### 대시보드 요약
```http
GET /api/dashboard/summary
Authorization: Bearer {token}

Response:
{
  "currentMonthCost": 1234.56,
  "projectedMonthCost": 3703.68,
  "totalResources": 45,
  "idleResources": 12,
  "totalRecommendations": 15,
  "potentialMonthlySaving": 890.12,
  "costTrend": [...],
  "topCostServices": [...]
}
```

---

## 🗂️ 공통 사항

### 응답 형식
- 모든 응답은 `application/json` 형식
- 날짜는 ISO 8601 형식 (YYYY-MM-DD)
- 통화는 기본적으로 USD

### 에러 응답
```json
{
  "message": "에러 메시지",
  "status": 404,
  "timestamp": "2025-01-13T12:34:56"
}
```

### 인증
- JWT Bearer 토큰 사용
- 토큰은 uid를 subject로 포함
- 인증 실패 시 `401 Unauthorized`

### 자동 데이터 수집 스케줄
- 리소스 정보: 매일 새벽 2시
- 비용 데이터: 매일 새벽 3시
- 리소스 메트릭: 매시간
- 월간 전체 수집: 매월 1일 새벽 4시

---

📌 **작성자:** 11조  
📆 **최종 수정일:** 2025.01.13
