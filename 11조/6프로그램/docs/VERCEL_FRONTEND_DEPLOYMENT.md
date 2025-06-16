# CostWise 프론트엔드 Vercel 배포 가이드

## 🚀 Vercel 배포 설정

### 1. Vercel 계정 및 프로젝트 생성
1. [Vercel](https://vercel.com)에 GitHub 계정으로 로그인
2. "New Project" 클릭
3. GitHub 레포지토리 연결: `industry-project-02-11`
4. 프로젝트 설정:
   - **Framework Preset**: Vite
   - **Root Directory**: `11조/6프로그램/apps/front`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`

### 2. 환경 변수 설정
Vercel 프로젝트 Settings → Environment Variables:
```
VITE_API=https://api.costwise.site
```

### 3. 도메인 연결
1. Vercel 프로젝트 Settings → Domains
2. "Add Domain" 클릭
3. `costwise.site` 입력
4. `www.costwise.site` 자동 추가됨

### 4. 가비아 DNS 설정
가비아 DNS 관리에서:
```
# A 레코드 (Vercel IP)
@ → 76.76.21.21
www → 76.76.21.21

# CNAME 레코드 (대체 방법)
@ → cname.vercel-dns.com
www → cname.vercel-dns.com
```

### 5. 자동 배포 설정
- `main` 브랜치에 푸시하면 자동 배포
- PR 생성 시 프리뷰 배포 생성

## 📱 프론트엔드 환경 설정

### 개발 환경 (.env)
```
VITE_API=http://localhost:8000
```

### 프로덕션 환경 (.env.production)
```
VITE_API=https://api.costwise.site
```

## 🔧 로컬 개발

```bash
cd 11조/6프로그램/apps/front
npm install
npm run dev
```

## 🎯 배포 확인
1. https://costwise.site 접속
2. 개발자 도구 Network 탭에서 API 호출 확인
3. API 엔드포인트가 https://api.costwise.site로 설정되었는지 확인

## 🐛 트러블슈팅

### CORS 에러
백엔드 nginx 설정에서 CORS 헤더 확인:
```
Access-Control-Allow-Origin: https://costwise.site
```

### 환경 변수가 적용되지 않음
1. Vercel 대시보드에서 환경 변수 재설정
2. Redeploy 실행

### 도메인 연결 안 됨
1. DNS 전파 시간 대기 (최대 48시간)
2. `nslookup costwise.site` 로 확인