# 클라우드 비용 최적화 도구 API 명세서

## 개요
- **Base URL**: `http://localhost:8000` (개발), `https://api.team11-cloud-cost.com` (운영)
- **API Gateway**: 모든 요청은 Gateway Service를 통해 라우팅
- **인증**: JWT Bearer Token (Auth Service에서 발급)

## 서비스별 라우팅
- `/auth-service/**` → Auth Service (포트 8082)
- `/user-service/**` → User Service (포트 8081)
- `/resource-service/**` → Backend Service (포트 8080)

---

## 1. 인증 서비스 (Auth Service)

### 1.1 로그인
```http
POST /auth-service/auth/login
Content-Type: application/json

{
  "uid": "string",
  "password": "string"
}

Response:
{
  "accessToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 3600
}

Set-Cookie: refresh_token=xxx; HttpOnly; Secure; SameSite=Strict
```

### 1.2 토큰 갱신
```http
POST /auth-service/auth/refresh
Cookie: refresh_token=xxx

Response:
{
  "accessToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 1.3 로그아웃
```http
POST /auth-service/auth/logout
Authorization: Bearer {token}
Cookie: refresh_token=xxx

Response: 204 No Content
```

---

## 2. 사용자 서비스 (User Service)

### 2.1 회원가입
```http
POST /user-service/users
Content-Type: application/json

{
  "uid": "string",
  "password": "string",
  "email": "string",
  "name": "string"
}

Response: 204 No Content
```

### 2.2 사용자 중복 확인
```http
GET /user-service/users/check?uid={uid}

Response: boolean
```

### 2.3 사용자 정보 조회
```http
GET /user-service/users/{uid}
Authorization: Bearer {token}

Response:
{
  "uid": "string",
  "email": "string",
  "name": "string",
  "createdAt": "2025-01-17T10:00:00Z"
}
```

### 2.4 사용자 정보 수정
```http
PUT /user-service/users/{uid}
Authorization: Bearer {token}
Content-Type: application/json

{
  "email": "string",
  "name": "string"
}

Response: 204 No Content
```

### 2.5 비밀번호 변경
```http
PUT /user-service/users/{uid}/password
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentPassword": "string",
  "newPassword": "string"
}

Response: 204 No Content
```

---

## 3. 리소스 서비스 (Backend Service)

### 3.1 AWS 계정 관리

#### 3.1.1 AWS 계정 등록
```http
POST /resource-service/api/aws-accounts
Authorization: Bearer {token}
Content-Type: application/json

{
  "accountAlias": "string",
  "awsAccountId": "string",
  "accessKeyId": "string",
  "secretAccessKey": "string",
  "region": "ap-northeast-2"
}

Response:
{
  "id": 1,
  "accountAlias": "string",
  "awsAccountId": "string",
  "region": "ap-northeast-2",
  "isActive": true,
  "createdAt": "2025-01-17T10:00:00Z"
}
```

#### 3.1.2 AWS 계정 목록 조회
```http
GET /resource-service/api/aws-accounts
Authorization: Bearer {token}

Response:
[
  {
    "id": 1,
    "accountAlias": "string",
    "awsAccountId": "string",
    "region": "ap-northeast-2",
    "isActive": true,
    "createdAt": "2025-01-17T10:00:00Z"
  }
]
```

#### 3.1.3 AWS 계정 삭제
```http
DELETE /resource-service/api/aws-accounts/{id}
Authorization: Bearer {token}

Response: 204 No Content
```

### 3.2 리소스 관리

#### 3.2.1 리소스 목록 조회
```http
GET /resource-service/api/resources?page=0&size=20&serviceType=EC2&isIdle=true
Authorization: Bearer {token}

Response:
{
  "content": [
    {
      "id": 1,
      "awsResourceId": "i-1234567890abcdef",
      "resourceName": "WebServer-1",
      "serviceType": "EC2",
      "instanceType": "t3.medium",
      "region": "ap-northeast-2",
      "isIdle": true,
      "usageRate": 5.2,
      "costDaily": 2.4,
      "costMonthly": 72.0,
      "status": "running",
      "lastCheckedAt": "2025-01-17T10:00:00Z"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

#### 3.2.2 리소스 상세 조회
```http
GET /resource-service/api/resources/{id}
Authorization: Bearer {token}

Response:
{
  "id": 1,
  "awsResourceId": "i-1234567890abcdef",
  "resourceName": "WebServer-1",
  "serviceType": "EC2",
  "instanceType": "t3.medium",
  "region": "ap-northeast-2",
  "isIdle": true,
  "usageRate": 5.2,
  "memoryUsageRate": 15.3,
  "networkUsageRate": 2.1,
  "costDaily": 2.4,
  "costMonthly": 72.0,
  "status": "running",
  "tags": {
    "Environment": "Production",
    "Owner": "TeamA"
  },
  "lastCheckedAt": "2025-01-17T10:00:00Z"
}
```

### 3.3 비용 분석

#### 3.3.1 비용 요약 조회
```http
GET /resource-service/api/cost/summary?startDate=2025-01-01&endDate=2025-01-31
Authorization: Bearer {token}

Response:
{
  "totalCost": 1250.50,
  "currency": "USD",
  "period": {
    "start": "2025-01-01",
    "end": "2025-01-31"
  },
  "byService": [
    {
      "serviceName": "Amazon EC2",
      "cost": 850.30,
      "percentage": 68.0
    },
    {
      "serviceName": "Amazon RDS",
      "cost": 300.20,
      "percentage": 24.0
    }
  ],
  "trend": {
    "previousPeriodCost": 1180.20,
    "change": 70.30,
    "changePercentage": 5.96
  }
}
```

#### 3.3.2 일별 비용 추이
```http
GET /resource-service/api/cost/daily?startDate=2025-01-01&endDate=2025-01-31
Authorization: Bearer {token}

Response:
[
  {
    "date": "2025-01-01",
    "cost": 40.50,
    "services": [
      {
        "serviceName": "Amazon EC2",
        "cost": 28.30
      }
    ]
  }
]
```

### 3.4 추천 관리

#### 3.4.1 추천 생성
```http
POST /resource-service/api/recommendations/generate
Authorization: Bearer {token}

Response:
{
  "message": "비용 최적화 추천이 생성되었습니다.",
  "count": 15,
  "totalExpectedSaving": 350.50
}
```

#### 3.4.2 추천 목록 조회
```http
GET /resource-service/api/recommendations?status=pending
Authorization: Bearer {token}

Response:
[
  {
    "id": 1,
    "resourceId": 1,
    "recommendationType": "downsize",
    "recommendationText": "EC2 인스턴스를 t3.medium에서 t3.small로 변경하면 월 $36 절감 가능",
    "currentCost": 72.0,
    "expectedCost": 36.0,
    "expectedSaving": 36.0,
    "savingPercentage": 50.0,
    "priority": "high",
    "status": "pending",
    "createdAt": "2025-01-17T10:00:00Z"
  }
]
```

#### 3.4.3 추천 수락/거절
```http
PUT /resource-service/api/recommendations/{id}/status
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": "accepted",  // or "ignored"
  "reason": "승인됨"
}

Response:
{
  "id": 1,
  "status": "accepted",
  "updatedAt": "2025-01-17T10:00:00Z"
}
```

#### 3.4.4 추천 요약 조회
```http
GET /resource-service/api/recommendations/summary
Authorization: Bearer {token}

Response:
{
  "totalRecommendations": 15,
  "totalExpectedSaving": 350.50,
  "currency": "USD",
  "monthlyProjectedSaving": 10515.00,
  "byType": {
    "downsize": 8,
    "stop": 4,
    "delete": 3
  }
}
```

### 3.5 AWS 데이터 수집

#### 3.5.1 전체 데이터 수집
```http
POST /resource-service/api/aws-data/collect-all/{awsAccountId}
Authorization: Bearer {token}

Response:
{
  "message": "데이터 수집이 시작되었습니다.",
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "estimatedTime": "5-10분"
}
```

#### 3.5.2 리소스만 수집
```http
POST /resource-service/api/aws-data/collect-resources/{awsAccountId}
Authorization: Bearer {token}

Response:
{
  "message": "리소스 수집이 시작되었습니다.",
  "resourceTypes": ["EC2", "RDS", "EBS"]
}
```

#### 3.5.3 비용 데이터만 수집
```http
POST /resource-service/api/aws-data/collect-costs/{awsAccountId}
Authorization: Bearer {token}

Response:
{
  "message": "비용 데이터 수집이 시작되었습니다.",
  "period": "최근 30일"
}
```

### 3.6 알림 관리

#### 3.6.1 알림 목록 조회
```http
GET /resource-service/api/alerts?isRead=false
Authorization: Bearer {token}

Response:
[
  {
    "id": 1,
    "alertType": "budget_exceed",
    "title": "월 예산 초과 경고",
    "message": "이번 달 AWS 비용이 설정된 예산의 90%를 초과했습니다.",
    "severity": "warning",
    "isRead": false,
    "sentAt": "2025-01-17T10:00:00Z"
  }
]
```

#### 3.6.2 알림 읽음 처리
```http
PUT /resource-service/api/alerts/{id}/read
Authorization: Bearer {token}

Response: 204 No Content
```

### 3.7 설정 관리

#### 3.7.1 설정 조회
```http
GET /resource-service/api/configs
Authorization: Bearer {token}

Response:
{
  "idleThreshold": 20.0,
  "budgetLimit": 1000,
  "alertEnabled": true
}
```

#### 3.7.2 설정 업데이트
```http
PUT /resource-service/api/configs
Authorization: Bearer {token}
Content-Type: application/json

{
  "idleThreshold": 15.0,
  "budgetLimit": 1500,
  "alertEnabled": true
}

Response: 204 No Content
```

---

## 공통 에러 응답

### 400 Bad Request
```json
{
  "timestamp": "2025-01-17T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "유효하지 않은 요청입니다.",
  "path": "/api/resources"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2025-01-17T10:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "인증이 필요합니다.",
  "path": "/api/resources"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2025-01-17T10:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "접근 권한이 없습니다.",
  "path": "/api/resources"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-01-17T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "리소스를 찾을 수 없습니다.",
  "path": "/api/resources/999"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2025-01-17T10:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "서버 오류가 발생했습니다.",
  "path": "/api/resources"
}
```
