# 🌐 클라우드 비용 최적화 도구

> 조선대학교 산학프로젝트1 02분반 11조  
> AWS 클라우드 리소스의 유휴 자원을 탐지하고 비용 절감 방안을 제시하는 통합 솔루션

## 📋 목차
- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [시스템 아키텍처](#-시스템-아키텍처)
- [시작하기](#-시작하기)
- [API 문서](#-api-문서)
- [배포 가이드](#-배포-가이드)
- [팀원 소개](#-팀원-소개)

## 🎯 프로젝트 소개

클라우드 서비스 사용이 증가하면서 많은 기업들이 불필요한 클라우드 비용을 지출하고 있습니다. 
본 프로젝트는 AWS 클라우드 환경에서 **유휴 리소스를 자동으로 탐지**하고 **비용 절감 방안을 제시**하여 
기업의 클라우드 운영 비용을 최적화하는 것을 목표로 합니다.

## ✨ 주요 기능

### 1. 🔍 리소스 모니터링
- EC2, RDS, EBS 등 주요 AWS 서비스 리소스 실시간 모니터링
- CPU, 메모리, 네트워크 사용률 분석
- 유휴 리소스 자동 탐지 (사용률 20% 미만)

### 2. 💰 비용 분석
- AWS Cost Explorer 연동을 통한 상세 비용 분석
- 일별/월별 비용 추이 시각화
- 서비스별 비용 분포 대시보드

### 3. 💡 최적화 추천
- 리소스 다운사이징 추천 (예: t3.large → t3.medium)
- 미사용 리소스 중지/삭제 제안
- 예상 절감액 계산 및 우선순위 제시

### 4. 🔔 알림 시스템
- 예산 초과 경고
- 유휴 리소스 감지 알림
- 이메일/Slack 연동 지원

### 5. 🔐 보안
- JWT 기반 인증
- AWS 크레덴셜 암호화 저장
- 세분화된 권한 관리

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.x, Spring Cloud
- **Language**: Java 17
- **Database**: PostgreSQL 13
- **Cache**: Redis 6.2
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway

### Frontend
- **Framework**: React 18
- **Build Tool**: Vite
- **State Management**: React Hooks
- **HTTP Client**: Axios
- **UI Components**: Custom CSS

### Infrastructure
- **Container**: Docker, Docker Compose
- **CI/CD**: GitHub Actions
- **Cloud**: AWS (EC2, RDS, S3, CloudFront, ECR)
- **Monitoring**: Spring Actuator, CloudWatch

## 🏗 시스템 아키텍처

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   React     │────▶│   API Gateway   │────▶│ Microservices   │
│   Web App   │     │  (Spring Cloud) │     │  ├─ Auth        │
└─────────────┘     └─────────────────┘     │  ├─ User        │
                                            │  └─ Backend     │
                                            └─────────────────┘
                                                     │
                                            ┌─────────────────┐
                                            │   PostgreSQL    │
                                            │     Redis       │
                                            └─────────────────┘
```

자세한 아키텍처는 [ARCHITECTURE.md](docs/ARCHITECTURE.md) 참조

## 🚀 시작하기

### 사전 요구사항
- Docker & Docker Compose
- Java 17+
- Node.js 18+
- AWS 계정 (선택사항)

### 로컬 개발 환경 실행

1. **프로젝트 클론**
```bash
git clone https://github.com/industry-project-02-11/industry-project-02-11.git
cd industry-project-02-11/11조/6프로그램
```

2. **환경 변수 설정**
```bash
cp .env.example .env
# .env 파일을 열어 필요한 값 설정
```

3. **데이터베이스 실행**
```bash
docker-compose up -d postgres redis
```

4. **백엔드 서비스 실행**
```bash
# 각 서비스 디렉토리에서
cd apps/eureka-discovery-service
./gradlew bootRun

# 새 터미널에서
cd apps/gateway-service
./gradlew bootRun

# 이하 auth-service, user-service, backend 동일
```

5. **프론트엔드 실행**
```bash
cd apps/front
npm install
npm run dev
```

6. **접속**
- Frontend: http://localhost:5173
- API Gateway: http://localhost:8000
- Eureka Dashboard: http://localhost:8761

## 📚 API 문서

주요 API 엔드포인트:

### 인증
- `POST /auth-service/auth/login` - 로그인
- `POST /auth-service/auth/refresh` - 토큰 갱신
- `POST /auth-service/auth/logout` - 로그아웃

### 리소스
- `GET /resource-service/api/resources` - 리소스 목록
- `POST /resource-service/api/aws-data/collect-all/{accountId}` - 데이터 수집

### 추천
- `POST /resource-service/api/recommendations/generate` - 추천 생성
- `GET /resource-service/api/recommendations` - 추천 목록

전체 API 명세는 [API-SPEC.md](docs/API-SPEC.md) 참조

## 🚢 배포 가이드

### AWS 배포
1. **사전 준비**
   - AWS CLI 설정
   - ECR 리포지토리 생성
   - RDS, ElastiCache 인스턴스 생성

2. **배포 실행**
```bash
./deploy.sh
```

자세한 배포 가이드는 [AWS-DEPLOYMENT-GUIDE.md](docs/AWS-DEPLOYMENT-GUIDE.md) 참조

### GitHub Actions CI/CD
- `main` 브랜치 푸시 시 자동 배포
- 필요한 GitHub Secrets 설정은 [GITHUB-SECRETS-SETUP.md](docs/GITHUB-SECRETS-SETUP.md) 참조

## 📊 프로젝트 구조

```
11조/
├── 1계획서/           # 프로젝트 계획서
├── 2결과보고서/       # 중간/최종 보고서
├── 3주간진도관리/     # 주차별 회의록
├── 4발표자료모음/     # 발표 자료
├── 5최종발표자료/     # 최종 발표
├── 6프로그램/         # 소스 코드 (본 디렉토리)
│   ├── apps/         # 마이크로서비스
│   ├── docs/         # 문서
│   ├── infra/        # 인프라 설정
│   └── scripts/      # 유틸리티 스크립트
├── 7논문/            # 논문 및 공모전 자료
├── 8포스터/          # 포스터 자료
└── 9기타/            # 기타 자료
```

## 👥 팀원 소개

| 이름 | 역할 | 담당 업무 | GitHub |
|------|------|-----------|--------|
| 김기욱 | 팀장, 백엔드 | 프로젝트 총괄, 백엔드 아키텍처 설계 | [@giuk](https://github.com/giuk) |
| 김준서 | 백엔드 | AWS 연동, 비용 분석 모듈 | [@kimjunser](https://github.com/kimjunser) |
| 나승빈 | 프론트엔드 | UI/UX 설계, React 개발 | [@naseungbin](https://github.com/naseungbin) |
| 이승현 | 백엔드 | 추천 알고리즘, 데이터베이스 설계 | [@seunghyun](https://github.com/seunghyun) |
| 정욱 | 인프라 | DevOps, CI/CD 구축 | [@jungwook](https://github.com/jungwook) |

## 📝 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 🤝 기여하기

프로젝트에 기여하고 싶으신가요? Pull Request는 언제나 환영입니다!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 문의

- 프로젝트 관련 문의: team11@example.com
- 버그 리포트: [Issues](https://github.com/industry-project-02-11/industry-project-02-11/issues)

---

**조선대학교 SW중심대학사업단**  
2025년 1학기 산학프로젝트1
