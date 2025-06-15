-- ========================================
-- 클라우드 비용 최적화 도구 DDL Script
-- 작성일: 2025-01-17
-- 팀: 조선대학교 산학프로젝트1 02분반 11조
-- ========================================

-- 1. 사용자 정보 테이블
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    uid VARCHAR(255) UNIQUE NOT NULL,                 -- 사용자 고유 ID (로그인용)
    email VARCHAR(255) UNIQUE NOT NULL,               -- 사용자 이메일
    password_hash VARCHAR(255) NOT NULL,              -- 해시된 비밀번호
    name VARCHAR(100),                                -- 사용자 이름
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 계정 생성 시간
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. AWS 계정 정보 테이블
CREATE TABLE aws_accounts (
    id SERIAL PRIMARY KEY,
    user_uid VARCHAR(255) NOT NULL,                   -- 소유 사용자
    account_alias VARCHAR(255),                       -- 계정 별칭
    aws_account_id VARCHAR(255) UNIQUE,               -- AWS 계정 ID
    access_key_id VARCHAR(255) NOT NULL,              -- AWS Access Key (암호화 저장)
    secret_access_key VARCHAR(255) NOT NULL,          -- AWS Secret Key (암호화 저장)
    region VARCHAR(50) DEFAULT 'ap-northeast-2',      -- 기본 리전
    is_active BOOLEAN DEFAULT true,                   -- 활성 상태
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE
);

-- 3. 사용자 설정 테이블
CREATE TABLE configs (
    id SERIAL PRIMARY KEY,
    user_uid VARCHAR(255) NOT NULL,                   -- 사용자 ID
    idle_threshold FLOAT DEFAULT 20.0,                -- 유휴 판단 기준 (%)
    budget_limit INTEGER,                             -- 월 예산 한도 (USD)
    alert_enabled BOOLEAN DEFAULT true,               -- 알림 활성화 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE
);

-- 4. 리소스 정보 테이블
CREATE TABLE resources (
    id SERIAL PRIMARY KEY,
    user_uid VARCHAR(255) NOT NULL,                   -- 리소스 소유자
    aws_account_id INTEGER,                           -- AWS 계정 ID
    aws_resource_id VARCHAR(100),                     -- AWS 리소스 ID (ex. i-123abc)
    resource_name VARCHAR(255),                       -- 리소스 이름
    service_type VARCHAR(50),                         -- 서비스 타입 (EC2, RDS, EBS 등)
    instance_type VARCHAR(50),                        -- 인스턴스 타입 (t3.micro 등)
    region VARCHAR(50),                               -- AWS 리전
    is_idle BOOLEAN DEFAULT FALSE,                    -- 유휴 상태 여부
    usage_rate FLOAT,                                 -- 최근 CPU 사용률 (%)
    memory_usage_rate FLOAT,                          -- 메모리 사용률 (%)
    network_usage_rate FLOAT,                         -- 네트워크 사용률 (%)
    cost_daily DECIMAL(15,4),                         -- 일일 비용 (USD)
    cost_monthly DECIMAL(15,4),                       -- 월간 비용 (USD)
    status VARCHAR(20),                               -- 상태 (running, stopped 등)
    tags JSONB,                                       -- AWS 태그 정보
    metadata JSONB,                                   -- 추가 메타데이터
    last_checked_at TIMESTAMP,                        -- 마지막 체크 시간
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
    FOREIGN KEY (aws_account_id) REFERENCES aws_accounts(id) ON DELETE CASCADE
);

-- 5. 비용 이력 테이블
CREATE TABLE cost_histories (
    id SERIAL PRIMARY KEY,
    user_uid VARCHAR(255) NOT NULL,                   -- 사용자 ID
    aws_account_id INTEGER,                           -- AWS 계정 ID
    service_name VARCHAR(100),                        -- AWS 서비스 이름
    resource_type VARCHAR(100),                       -- 리소스 타입
    cost DECIMAL(15,4),                              -- 비용
    cost_usd DECIMAL(15,4),                          -- USD 비용
    currency VARCHAR(10) DEFAULT 'USD',              -- 통화
    usage_date DATE,                                  -- 비용 발생 날짜
    usage_quantity DECIMAL(15,4),                    -- 사용량
    usage_unit VARCHAR(50),                           -- 사용 단위
    raw_data JSONB,                                   -- AWS 원본 데이터
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
    FOREIGN KEY (aws_account_id) REFERENCES aws_accounts(id) ON DELETE CASCADE
);

-- 6. 절감 추천 테이블
CREATE TABLE recommendations (
    id SERIAL PRIMARY KEY,
    resource_id INTEGER,                              -- 대상 리소스
    user_uid VARCHAR(255) NOT NULL,                   -- 사용자 ID
    recommendation_type VARCHAR(50),                  -- 추천 타입 (downsize, stop, delete 등)
    recommendation_text TEXT,                         -- 추천 설명
    current_cost DECIMAL(15,4),                      -- 현재 비용
    expected_cost DECIMAL(15,4),                      -- 예상 비용
    expected_saving DECIMAL(15,4),                    -- 예상 절감액 (USD)
    saving_percentage FLOAT,                          -- 절감 비율 (%)
    priority VARCHAR(20),                             -- 우선순위 (high, medium, low)
    status VARCHAR(20) DEFAULT 'pending',             -- 상태 (pending, accepted, ignored, applied)
    applied_at TIMESTAMP,                             -- 적용 시간
    metadata JSONB,                                   -- 추가 정보
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE,
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE
);

-- 7. 추천 로그 테이블
CREATE TABLE recommendation_logs (
    id SERIAL PRIMARY KEY,
    recommendation_id INTEGER,                        -- 추천 ID
    user_uid VARCHAR(255) NOT NULL,                   -- 수행 사용자
    action VARCHAR(50),                               -- 액션 (accept, ignore, apply)
    reason TEXT,                                      -- 사유
    metadata JSONB,                                   -- 추가 정보
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recommendation_id) REFERENCES recommendations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE
);

-- 8. 알림 기록 테이블
CREATE TABLE alerts (
    id SERIAL PRIMARY KEY,
    user_uid VARCHAR(255) NOT NULL,                   -- 수신 사용자
    alert_type VARCHAR(50),                           -- 알림 타입 (budget_exceed, idle_resource 등)
    title VARCHAR(255),                               -- 알림 제목
    message TEXT,                                     -- 알림 내용
    severity VARCHAR(20),                             -- 심각도 (info, warning, critical)
    is_read BOOLEAN DEFAULT FALSE,                    -- 읽음 여부
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,      -- 전송 시간
    channel VARCHAR(50),                              -- 전송 채널 (email, slack, in-app)
    metadata JSONB,                                   -- 추가 정보
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE
);

-- 9. 감사 로그 테이블
CREATE TABLE audit_logs (
    id SERIAL PRIMARY KEY,
    user_uid VARCHAR(255) NOT NULL,                   -- 수행 사용자
    action VARCHAR(100),                              -- 수행 액션
    target_type VARCHAR(100),                         -- 대상 타입
    target_id INTEGER,                                -- 대상 ID
    ip_address VARCHAR(45),                           -- IP 주소
    user_agent TEXT,                                  -- User Agent
    request_method VARCHAR(10),                       -- HTTP Method
    request_url TEXT,                                 -- 요청 URL
    response_status INTEGER,                          -- 응답 상태 코드
    meta JSONB,                                       -- 부가 정보
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE
);

-- ========================================
-- 인덱스 생성
-- ========================================

-- users 테이블 인덱스
CREATE INDEX idx_users_uid ON users(uid);
CREATE INDEX idx_users_email ON users(email);

-- aws_accounts 테이블 인덱스
CREATE INDEX idx_aws_accounts_user_uid ON aws_accounts(user_uid);
CREATE INDEX idx_aws_accounts_aws_account_id ON aws_accounts(aws_account_id);

-- configs 테이블 인덱스
CREATE INDEX idx_configs_user_uid ON configs(user_uid);

-- resources 테이블 인덱스
CREATE INDEX idx_resources_user_uid ON resources(user_uid);
CREATE INDEX idx_resources_aws_account_id ON resources(aws_account_id);
CREATE INDEX idx_resources_service_type ON resources(service_type);
CREATE INDEX idx_resources_is_idle ON resources(is_idle);
CREATE INDEX idx_resources_aws_resource_id ON resources(aws_resource_id);

-- cost_histories 테이블 인덱스
CREATE INDEX idx_cost_histories_user_uid ON cost_histories(user_uid);
CREATE INDEX idx_cost_histories_aws_account_id ON cost_histories(aws_account_id);
CREATE INDEX idx_cost_histories_usage_date ON cost_histories(usage_date);
CREATE INDEX idx_cost_histories_service_name ON cost_histories(service_name);

-- recommendations 테이블 인덱스
CREATE INDEX idx_recommendations_user_uid ON recommendations(user_uid);
CREATE INDEX idx_recommendations_resource_id ON recommendations(resource_id);
CREATE INDEX idx_recommendations_status ON recommendations(status);

-- alerts 테이블 인덱스
CREATE INDEX idx_alerts_user_uid ON alerts(user_uid);
CREATE INDEX idx_alerts_is_read ON alerts(is_read);
CREATE INDEX idx_alerts_sent_at ON alerts(sent_at);

-- audit_logs 테이블 인덱스
CREATE INDEX idx_audit_logs_user_uid ON audit_logs(user_uid);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
