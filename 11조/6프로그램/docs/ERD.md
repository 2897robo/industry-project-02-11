# 클라우드 비용 최적화 도구 ERD

## Entity Relationship Diagram

```mermaid
erDiagram
    USERS ||--o{ AWS_ACCOUNTS : "owns"
    USERS ||--o| CONFIGS : "has"
    USERS ||--o{ RESOURCES : "owns"
    USERS ||--o{ COST_HISTORIES : "has"
    USERS ||--o{ RECOMMENDATIONS : "receives"
    USERS ||--o{ RECOMMENDATION_LOGS : "performs"
    USERS ||--o{ ALERTS : "receives"
    USERS ||--o{ AUDIT_LOGS : "generates"
    
    AWS_ACCOUNTS ||--o{ RESOURCES : "contains"
    AWS_ACCOUNTS ||--o{ COST_HISTORIES : "generates"
    
    RESOURCES ||--o{ RECOMMENDATIONS : "has"
    RECOMMENDATIONS ||--o{ RECOMMENDATION_LOGS : "logs"

    USERS {
        serial id PK
        varchar uid UK "unique user identifier"
        varchar email UK
        varchar password_hash
        varchar name
        timestamp created_at
        timestamp updated_at
    }

    AWS_ACCOUNTS {
        serial id PK
        varchar user_uid FK
        varchar account_alias
        varchar aws_account_id UK
        varchar access_key_id "encrypted"
        varchar secret_access_key "encrypted"
        varchar region
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }

    CONFIGS {
        serial id PK
        varchar user_uid FK
        float idle_threshold "default 20.0"
        integer budget_limit "in USD"
        boolean alert_enabled
        timestamp created_at
        timestamp updated_at
    }

    RESOURCES {
        serial id PK
        varchar user_uid FK
        integer aws_account_id FK
        varchar aws_resource_id
        varchar resource_name
        varchar service_type "EC2, RDS, EBS"
        varchar instance_type
        varchar region
        boolean is_idle
        float usage_rate "CPU %"
        float memory_usage_rate
        float network_usage_rate
        decimal cost_daily
        decimal cost_monthly
        varchar status
        jsonb tags
        jsonb metadata
        timestamp last_checked_at
        timestamp created_at
        timestamp updated_at
    }

    COST_HISTORIES {
        serial id PK
        varchar user_uid FK
        integer aws_account_id FK
        varchar service_name
        varchar resource_type
        decimal cost
        decimal cost_usd
        varchar currency "default USD"
        date usage_date
        decimal usage_quantity
        varchar usage_unit
        jsonb raw_data
        timestamp created_at
        timestamp updated_at
    }

    RECOMMENDATIONS {
        serial id PK
        integer resource_id FK
        varchar user_uid FK
        varchar recommendation_type
        text recommendation_text
        decimal current_cost
        decimal expected_cost
        decimal expected_saving
        float saving_percentage
        varchar priority "high, medium, low"
        varchar status "pending, accepted, ignored"
        timestamp applied_at
        jsonb metadata
        timestamp created_at
        timestamp updated_at
    }

    RECOMMENDATION_LOGS {
        serial id PK
        integer recommendation_id FK
        varchar user_uid FK
        varchar action "accept, ignore, apply"
        text reason
        jsonb metadata
        timestamp created_at
    }

    ALERTS {
        serial id PK
        varchar user_uid FK
        varchar alert_type
        varchar title
        text message
        varchar severity "info, warning, critical"
        boolean is_read
        timestamp sent_at
        varchar channel "email, slack, in-app"
        jsonb metadata
    }

    AUDIT_LOGS {
        serial id PK
        varchar user_uid FK
        varchar action
        varchar target_type
        integer target_id
        varchar ip_address
        text user_agent
        varchar request_method
        text request_url
        integer response_status
        jsonb meta
        timestamp created_at
    }
```

## 주요 관계 설명

### 1. 사용자 (USERS)
- 시스템의 핵심 엔티티로 모든 데이터의 소유자
- `uid`를 통해 마이크로서비스 간 사용자 식별
- 여러 AWS 계정을 등록하고 관리 가능

### 2. AWS 계정 (AWS_ACCOUNTS)
- 사용자가 등록한 AWS 계정 정보
- Access Key와 Secret Key는 암호화되어 저장
- 각 계정별로 리소스와 비용 데이터 수집

### 3. 리소스 (RESOURCES)
- AWS에서 수집된 실제 클라우드 리소스
- EC2, RDS, EBS 등 다양한 서비스 타입 지원
- 유휴 상태 및 사용률 정보 저장

### 4. 비용 이력 (COST_HISTORIES)
- AWS Cost Explorer API를 통해 수집된 비용 데이터
- 일별 비용 추적 및 분석
- 서비스별, 리소스별 비용 세분화

### 5. 추천 (RECOMMENDATIONS)
- 리소스 분석을 통한 비용 절감 추천
- 다운사이징, 중지, 삭제 등 다양한 추천 타입
- 예상 절감액 및 우선순위 정보 제공

### 6. 추천 로그 (RECOMMENDATION_LOGS)
- 사용자의 추천 수락/거절 이력
- 추천 효과성 분석을 위한 데이터

### 7. 알림 (ALERTS)
- 예산 초과, 유휴 리소스 감지 등 다양한 알림
- 이메일, Slack, 인앱 등 다중 채널 지원

### 8. 감사 로그 (AUDIT_LOGS)
- 시스템 내 모든 중요 활동 추적
- 보안 및 컴플라이언스를 위한 로깅

## 데이터베이스 특징

- **PostgreSQL** 사용
- **JSONB** 타입으로 유연한 메타데이터 저장
- 적절한 **인덱스**로 쿼리 성능 최적화
- **외래 키 제약**으로 데이터 무결성 보장
- **Cascade Delete**로 관련 데이터 자동 정리
