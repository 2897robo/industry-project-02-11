-- ✅ 사용자 정보 테이블
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,                            -- 고유 사용자 ID
                       email VARCHAR(255) UNIQUE NOT NULL,               -- 사용자 이메일 (로그인용)
                       password_hash VARCHAR(255) NOT NULL,              -- 해시된 비밀번호
                       name VARCHAR(100),                                -- 사용자 이름
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP    -- 계정 생성 시간
);

-- ✅ 사용자 설정 테이블 (유휴 기준, 예산 한도 등)
CREATE TABLE configs (
                         id SERIAL PRIMARY KEY,
                         user_id INTEGER REFERENCES users(id),             -- 소유 사용자 ID
                         idle_threshold FLOAT DEFAULT 20.0,                -- 유휴 판단 기준 (%)
                         budget_limit INTEGER,                             -- 월 예산 한도 (원화 또는 USD)
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ✅ 리소스(EC2, RDS 등) 정보 테이블
CREATE TABLE resources (
                           id SERIAL PRIMARY KEY,
                           user_id INTEGER REFERENCES users(id),             -- 리소스 소유자
                           aws_resource_id VARCHAR(100),                     -- AWS 리소스 ID (ex. i-123abc)
                           service_type VARCHAR(50),                         -- 서비스 타입 (EC2, RDS, EBS 등)
                           region VARCHAR(50),                               -- AWS 리전 정보 (ap-northeast-2 등)
                           is_idle BOOLEAN DEFAULT FALSE,                    -- 유휴 상태 여부
                           usage_rate FLOAT,                                 -- 최근 CPU 사용률 등 사용률 %
                           cost_usd FLOAT,                                   -- 최근 비용 (USD)
                           last_checked_at TIMESTAMP,                        -- 마지막 분석/탐지 시간
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ✅ 월별 비용 이력 테이블
CREATE TABLE cost_histories (
                                id SERIAL PRIMARY KEY,
                                user_id INTEGER REFERENCES users(id),             -- 사용자 ID
                                service_name VARCHAR(100),                        -- AWS 서비스 이름 (예: Amazon EC2)
                                cost_usd FLOAT,                                   -- 해당 일자의 비용
                                usage_date DATE,                                  -- 비용 발생 날짜
                                raw_data JSONB,                                   -- AWS에서 받은 원본 JSON
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ✅ 절감 추천 테이블
CREATE TABLE recommendations (
                                 id SERIAL PRIMARY KEY,
                                 resource_id INTEGER REFERENCES resources(id),     -- 해당 리소스
                                 recommendation_text TEXT,                         -- 추천 메시지 (예: t3.small로 다운사이징)
                                 expected_saving FLOAT,                            -- 절감 예상 금액 (USD)
                                 status VARCHAR(20) DEFAULT 'pending',             -- 상태: pending, accepted, ignored
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ✅ 추천 결과 기록 로그 테이블
CREATE TABLE recommendation_logs (
                                     id SERIAL PRIMARY KEY,
                                     recommendation_id INTEGER REFERENCES recommendations(id),
                                     user_id INTEGER REFERENCES users(id),             -- 수행한 사용자
                                     action VARCHAR(50),                               -- 액션 (accept, ignore 등)
                                     reason TEXT,                                      -- 이유 (선택 입력)
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ✅ 알림 전송 기록 테이블
CREATE TABLE alerts (
                        id SERIAL PRIMARY KEY,
                        user_id INTEGER REFERENCES users(id),             -- 수신 사용자
                        alert_type VARCHAR(50),                           -- 유형 (예산 초과, 유휴 지속 등)
                        message TEXT,                                     -- 알림 내용
                        sent_at TIMESTAMP,                                -- 알림 전송 시간
                        channel VARCHAR(50)                               -- 전송 채널 (Slack, Email 등)
);

-- ✅ 감사 로그 테이블 (관리자 또는 사용자 행위 기록)
CREATE TABLE audit_logs (
                            id SERIAL PRIMARY KEY,
                            user_id INTEGER REFERENCES users(id),             -- 수행한 사용자
                            action VARCHAR(100),                              -- 수행한 액션 명 (예: login, update_config)
                            target_type VARCHAR(100),                         -- 대상 타입 (resource, config 등)
                            target_id INTEGER,                                -- 대상 ID
                            meta JSONB,                                       -- 부가 정보 (IP, 변경 전/후 데이터 등)
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ❌ [선택] 사용자 피드백 (현재는 사용 X 예정)
-- CREATE TABLE feedback (
--     id SERIAL PRIMARY KEY,
--     user_id INTEGER REFERENCES users(id),
--     recommendation_id INTEGER REFERENCES recommendations(id),
--     rating INTEGER CHECK (rating BETWEEN 1 AND 5),
--     comment TEXT,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- );
