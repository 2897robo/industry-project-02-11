# AWS 배포 자동화 가이드

## 📋 개요

이 프로젝트는 Team 11 Cloud Cost Optimization Platform을 AWS에 완전 자동화된 방식으로 배포합니다.

## 🚀 빠른 시작

### 사전 요구사항

1. AWS CLI 설치 및 설정
```bash
aws configure
```

2. 필요한 권한
- EC2, VPC, RDS, ElastiCache, IAM, ECR 전체 권한

### 전체 배포 실행

```bash
# 실행 권한 부여
chmod +x deploy-all.sh

# 전체 배포 시작
./deploy-all.sh
```

## 📁 스크립트 설명

### 1. `deploy-all.sh`
- **마스터 배포 스크립트**
- 모든 배포 단계를 순차적으로 실행
- 각 단계별로 실행 여부를 선택 가능

### 2. `setup-iam-roles.sh`
- EC2가 ECR에 접근할 수 있는 IAM 역할 생성
- 인스턴스 프로파일 설정

### 3. `setup-ecr-repositories.sh`
- ECR 리포지토리 확인 및 생성
- 필요한 모든 서비스의 리포지토리 설정

### 4. `build-and-push-to-ecr.sh`
- 모든 마이크로서비스 빌드
- Docker 이미지 생성 및 ECR 푸시

### 5. `aws-deploy-complete.sh`
- **핵심 인프라 구축 스크립트**
- 생성하는 리소스:
  - VPC 및 서브넷
  - 인터넷 게이트웨이
  - 보안 그룹
  - RDS PostgreSQL
  - ElastiCache Redis
  - EC2 인스턴스
  - Elastic IP

### 6. `manage-deployment.sh`
- 배포 후 관리 도구
- 서비스 모니터링, 로그 확인, 백업 등

## 🏗️ 아키텍처

```
┌─────────────────┐
│   Route 53      │
└────────┬────────┘
         │
┌────────▼────────┐
│   CloudFront    │ ──► S3 (Frontend)
└────────┬────────┘
         │
┌────────▼────────┐
│   ALB/NLB       │
└────────┬────────┘
         │
┌────────▼────────┐     ┌──────────────┐
│   EC2 Instance  │ ──► │ ElastiCache  │
│                 │     │   (Redis)    │
│ ┌─────────────┐ │     └──────────────┘
│ │   Docker    │ │
│ │ ┌─────────┐ │ │     ┌──────────────┐
│ │ │ Eureka  │ │ │     │     RDS      │
│ │ ├─────────┤ │ │ ──► │ (PostgreSQL) │
│ │ │ Gateway │ │ │     └──────────────┘
│ │ ├─────────┤ │ │
│ │ │Services │ │ │
│ │ └─────────┘ │ │
│ └─────────────┘ │
└─────────────────┘
```

## 💰 비용 예상

### 월간 예상 비용 (최소 구성)
- EC2 t3.large: ~$60
- RDS db.t3.micro: ~$15
- ElastiCache cache.t3.micro: ~$12
- EBS 30GB: ~$3
- 데이터 전송: ~$10
- **총합: 약 $100/월**

## 🔧 환경 변수

`.env.production` 파일이 자동 생성되며 다음 정보를 포함합니다:

```env
DB_URL=jdbc:postgresql://[RDS엔드포인트]:5432/team11_cloud_cost
DB_USERNAME=postgres
DB_PASSWORD=[자동생성된 비밀번호]
REDIS_HOST=[Redis엔드포인트]
REDIS_PORT=6379
JWT_SECRET=[자동생성된 시크릿]
AES_KEY=[자동생성된 키]
ECR_REGISTRY=[ECR 레지스트리 URL]
```

## 📊 모니터링

### 서비스 확인
```bash
# 관리 도구 실행
./manage-deployment.sh

# 옵션:
# 1. 서비스 상태 확인
# 2. 로그 보기
# 3. 서비스 재시작
# 등...
```

### 접속 URL
- API Gateway: `http://[EC2-IP]:8000`
- Eureka Dashboard: `http://[EC2-IP]:8761`
- 각 서비스 Health: `http://[EC2-IP]:8000/[service-name]/actuator/health`

## 🔒 보안 고려사항

1. **키 관리**
   - `team11-cloud-cost-key.pem` 파일은 안전하게 보관
   - `.gitignore`에 추가 필수

2. **보안 그룹**
   - 필요한 포트만 오픈
   - SSH는 관리자 IP만 허용 권장

3. **암호화**
   - RDS 암호화 스토리지 사용 권장
   - SSL/TLS 인증서 적용 필요

## 🛠️ 트러블슈팅

### EC2 인스턴스 접속 안될 때
```bash
# 키 파일 권한 확인
chmod 600 team11-cloud-cost-key.pem

# 보안 그룹 확인
aws ec2 describe-security-groups --group-ids [보안그룹ID]
```

### Docker 컨테이너가 시작되지 않을 때
```bash
# EC2 접속 후
sudo systemctl status docker
docker-compose -f docker-compose.prod.yml logs
```

### RDS 연결 실패
- 보안 그룹 인바운드 규칙 확인
- RDS 엔드포인트 및 포트 확인
- 네트워크 ACL 확인

## 🗑️ 리소스 정리

```bash
# 관리 도구에서 옵션 9 선택
./manage-deployment.sh

# 또는 직접 실행
./delete-all-resources.sh
```

⚠️ **주의**: 모든 데이터가 삭제되므로 백업 필수!

## 📞 지원

문제 발생 시:
1. 로그 확인: `docker-compose logs`
2. AWS 콘솔에서 리소스 상태 확인
3. CloudWatch 로그 확인

---

**작성일**: 2025년 1월
**버전**: 1.0.0
