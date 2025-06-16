# CostWise AWS 배포 가이드

## 📋 목차
1. [AWS 리소스 준비](#1-aws-리소스-준비)
2. [EC2 인스턴스 설정](#2-ec2-인스턴스-설정)
3. [RDS PostgreSQL 설정](#3-rds-postgresql-설정)
4. [ElastiCache Redis 설정](#4-elasticache-redis-설정)
5. [ECR 레포지토리 생성](#5-ecr-레포지토리-생성)
6. [도메인 및 SSL 설정](#6-도메인-및-ssl-설정)
7. [GitHub Actions 설정](#7-github-actions-설정)
8. [최종 배포](#8-최종-배포)

## 1. AWS 리소스 준비

### 1.1 VPC 및 네트워크 설정
```bash
# VPC 생성 (10.0.0.0/16)
# - Public Subnet: 10.0.1.0/24 (EC2, NAT Gateway)
# - Private Subnet A: 10.0.2.0/24 (RDS Primary)
# - Private Subnet B: 10.0.3.0/24 (RDS Standby)
# - Private Subnet C: 10.0.4.0/24 (ElastiCache)
```

### 1.2 보안 그룹 생성
```bash
# 1. EC2 보안 그룹 (costwise-ec2-sg)
- SSH (22): 관리자 IP만
- HTTP (80): 0.0.0.0/0
- HTTPS (443): 0.0.0.0/0
- Eureka (8761): VPC 내부만

# 2. RDS 보안 그룹 (costwise-rds-sg)
- PostgreSQL (5432): EC2 보안 그룹에서만

# 3. ElastiCache 보안 그룹 (costwise-redis-sg)
- Redis (6379): EC2 보안 그룹에서만
```

## 2. EC2 인스턴스 설정

### 2.1 EC2 인스턴스 생성
- **AMI**: Amazon Linux 2023
- **인스턴스 타입**: t3.medium (최소 사양)
- **스토리지**: 30GB gp3
- **키 페어**: 새로 생성 후 안전하게 보관

### 2.2 Elastic IP 할당
```bash
# Elastic IP 생성 후 EC2 인스턴스에 연결
# 이 IP를 Route 53에 등록할 예정
```

### 2.3 EC2 초기 설정
```bash
# SSH 접속
ssh -i your-key.pem ec2-user@[EC2-ELASTIC-IP]

# 초기 설정 스크립트 실행
curl -O https://raw.githubusercontent.com/industry-project-02-11/industry-project-02-11/main/11조/6프로그램/scripts/ec2-init.sh
chmod +x ec2-init.sh
./ec2-init.sh
```

## 3. RDS PostgreSQL 설정

### 3.1 RDS 인스턴스 생성
- **엔진**: PostgreSQL 15.x
- **템플릿**: 프리 티어 (개발/테스트)
- **인스턴스 클래스**: db.t3.micro
- **스토리지**: 20GB gp3
- **다중 AZ**: 비활성화 (비용 절감)

### 3.2 데이터베이스 설정
- **DB 이름**: costwise_db
- **마스터 사용자**: costwise_admin
- **비밀번호**: 강력한 비밀번호 생성

### 3.3 파라미터 그룹 설정
```sql
-- 연결 수 증가
max_connections = 200

-- 한글 설정
lc_messages = 'ko_KR.UTF-8'
lc_monetary = 'ko_KR.UTF-8'
lc_numeric = 'ko_KR.UTF-8'
lc_time = 'ko_KR.UTF-8'
```

## 4. ElastiCache Redis 설정

### 4.1 Redis 클러스터 생성
- **노드 타입**: cache.t3.micro
- **복제본 수**: 0 (단일 노드)
- **파라미터 그룹**: default.redis7

### 4.2 Redis 설정
```bash
# 기본 설정으로 충분
# 필요시 파라미터 그룹에서 조정
maxmemory-policy: allkeys-lru
```

## 5. ECR 레포지토리 생성

### 5.1 레포지토리 생성
```bash
# AWS CLI로 생성
aws ecr create-repository --repository-name team11-cloud-cost-backend --region ap-northeast-2
aws ecr create-repository --repository-name team11-cloud-cost-auth --region ap-northeast-2
aws ecr create-repository --repository-name team11-cloud-cost-user --region ap-northeast-2
aws ecr create-repository --repository-name team11-cloud-cost-gateway --region ap-northeast-2
aws ecr create-repository --repository-name team11-cloud-cost-eureka --region ap-northeast-2
aws ecr create-repository --repository-name team11-cloud-cost-frontend --region ap-northeast-2