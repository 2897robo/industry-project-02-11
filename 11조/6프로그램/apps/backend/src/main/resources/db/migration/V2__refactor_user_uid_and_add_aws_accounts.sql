-- 1. users 테이블에 uid 컬럼 추가
ALTER TABLE users ADD COLUMN uid VARCHAR(255) UNIQUE;

-- 기존 데이터가 있다면 임시 uid 생성 (실제로는 적절한 마이그레이션 필요)
UPDATE users SET uid = 'user_' || id WHERE uid IS NULL;

-- uid를 NOT NULL로 변경
ALTER TABLE users ALTER COLUMN uid SET NOT NULL;

-- 2. AWS 계정 정보 테이블 생성
CREATE TABLE aws_accounts (
    id SERIAL PRIMARY KEY,
    user_uid VARCHAR(255) NOT NULL,
    account_alias VARCHAR(255),
    aws_account_id VARCHAR(255) UNIQUE,
    access_key_id VARCHAR(255) NOT NULL,
    secret_access_key VARCHAR(255) NOT NULL,  -- 암호화된 상태로 저장
    region VARCHAR(50) DEFAULT 'ap-northeast-2',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_uid) REFERENCES users(uid)
);

-- 인덱스 추가
CREATE INDEX idx_aws_accounts_user_uid ON aws_accounts(user_uid);

-- 3. 기존 테이블들의 user_id를 user_uid로 변경
-- configs 테이블
ALTER TABLE configs ADD COLUMN user_uid VARCHAR(255);
UPDATE configs SET user_uid = (SELECT uid FROM users WHERE users.id = configs.user_id);
ALTER TABLE configs ALTER COLUMN user_uid SET NOT NULL;
ALTER TABLE configs DROP CONSTRAINT configs_user_id_fkey;
ALTER TABLE configs DROP COLUMN user_id;
ALTER TABLE configs ADD CONSTRAINT configs_user_uid_fkey FOREIGN KEY (user_uid) REFERENCES users(uid);

-- resources 테이블
ALTER TABLE resources ADD COLUMN user_uid VARCHAR(255);
UPDATE resources SET user_uid = (SELECT uid FROM users WHERE users.id = resources.user_id);
ALTER TABLE resources ALTER COLUMN user_uid SET NOT NULL;
ALTER TABLE resources DROP CONSTRAINT resources_user_id_fkey;
ALTER TABLE resources DROP COLUMN user_id;
ALTER TABLE resources ADD CONSTRAINT resources_user_uid_fkey FOREIGN KEY (user_uid) REFERENCES users(uid);

-- cost_histories 테이블
ALTER TABLE cost_histories ADD COLUMN user_uid VARCHAR(255);
ALTER TABLE cost_histories ADD COLUMN aws_account_id INTEGER;
UPDATE cost_histories SET user_uid = (SELECT uid FROM users WHERE users.id = cost_histories.user_id);
ALTER TABLE cost_histories ALTER COLUMN user_uid SET NOT NULL;
ALTER TABLE cost_histories DROP CONSTRAINT cost_histories_user_id_fkey;
ALTER TABLE cost_histories DROP COLUMN user_id;
ALTER TABLE cost_histories ADD CONSTRAINT cost_histories_user_uid_fkey FOREIGN KEY (user_uid) REFERENCES users(uid);
ALTER TABLE cost_histories ADD CONSTRAINT cost_histories_aws_account_id_fkey FOREIGN KEY (aws_account_id) REFERENCES aws_accounts(id);

-- 추가 컬럼들
ALTER TABLE cost_histories ADD COLUMN resource_type VARCHAR(100);
ALTER TABLE cost_histories ADD COLUMN cost DECIMAL(15,4);
ALTER TABLE cost_histories ADD COLUMN currency VARCHAR(10) DEFAULT 'USD';
ALTER TABLE cost_histories ALTER COLUMN cost_usd TYPE DECIMAL(15,4);
ALTER TABLE cost_histories ADD COLUMN updated_at TIMESTAMP;

-- recommendation_logs 테이블
ALTER TABLE recommendation_logs ADD COLUMN user_uid VARCHAR(255);
UPDATE recommendation_logs SET user_uid = (SELECT uid FROM users WHERE users.id = recommendation_logs.user_id);
ALTER TABLE recommendation_logs ALTER COLUMN user_uid SET NOT NULL;
ALTER TABLE recommendation_logs DROP CONSTRAINT recommendation_logs_user_id_fkey;
ALTER TABLE recommendation_logs DROP COLUMN user_id;
ALTER TABLE recommendation_logs ADD CONSTRAINT recommendation_logs_user_uid_fkey FOREIGN KEY (user_uid) REFERENCES users(uid);

-- alerts 테이블 (user_id를 user_uid로 변경)
ALTER TABLE alerts ADD COLUMN user_uid VARCHAR(255);
UPDATE alerts SET user_uid = (SELECT uid FROM users WHERE users.id = alerts.user_id);
ALTER TABLE alerts ALTER COLUMN user_uid SET NOT NULL;
ALTER TABLE alerts DROP CONSTRAINT alerts_user_id_fkey;
ALTER TABLE alerts DROP COLUMN user_id;
ALTER TABLE alerts ADD CONSTRAINT alerts_user_uid_fkey FOREIGN KEY (user_uid) REFERENCES users(uid);

-- audit_logs 테이블
ALTER TABLE audit_logs ADD COLUMN user_uid VARCHAR(255);
UPDATE audit_logs SET user_uid = (SELECT uid FROM users WHERE users.id = audit_logs.user_id);
ALTER TABLE audit_logs ALTER COLUMN user_uid SET NOT NULL;
ALTER TABLE audit_logs DROP CONSTRAINT audit_logs_user_id_fkey;
ALTER TABLE audit_logs DROP COLUMN user_id;
ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_user_uid_fkey FOREIGN KEY (user_uid) REFERENCES users(uid);

-- 인덱스 생성
CREATE INDEX idx_configs_user_uid ON configs(user_uid);
CREATE INDEX idx_resources_user_uid ON resources(user_uid);
CREATE INDEX idx_cost_histories_user_uid ON cost_histories(user_uid);
CREATE INDEX idx_cost_histories_usage_date ON cost_histories(usage_date);
CREATE INDEX idx_recommendation_logs_user_uid ON recommendation_logs(user_uid);
CREATE INDEX idx_alerts_user_uid ON alerts(user_uid);
CREATE INDEX idx_audit_logs_user_uid ON audit_logs(user_uid);
