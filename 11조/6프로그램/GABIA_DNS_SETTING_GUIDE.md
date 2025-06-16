# 가비아 DNS 설정 가이드

## 1. 가비아 관리 페이지 접속
1. https://www.gabia.com 접속
2. 로그인
3. My 가비아 → 서비스 관리 → 도메인

## 2. DNS 설정
1. `costwise.site` 도메인 찾기
2. `DNS 설정` 버튼 클릭
3. `DNS 레코드 관리` 선택

## 3. 레코드 추가

### A 레코드 추가
다음 레코드들을 추가하세요:

| 타입 | 호스트 | 값 (EC2 Public IP) | TTL |
|------|--------|-------------------|-----|
| A    | @      | [EC2-IP]          | 300 |
| A    | api    | [EC2-IP]          | 300 |
| A    | www    | [EC2-IP]          | 300 |

### 기존 레코드 삭제
- 기존에 설정된 A 레코드가 있다면 삭제

## 4. 설정 저장
- 모든 레코드 추가 후 `저장` 클릭
- DNS 전파에는 최대 48시간이 걸릴 수 있지만, 보통 10-30분 내에 적용됩니다.

## 5. DNS 전파 확인
터미널에서 다음 명령어로 확인:
```bash
# DNS 조회
nslookup costwise.site
nslookup api.costwise.site
nslookup www.costwise.site

# 또는
dig costwise.site
dig api.costwise.site
```

## 6. Vercel 프론트엔드 설정 (이미 설정되어 있다면 스킵)

### Vercel 도메인 설정
1. Vercel 대시보드 접속
2. 프로젝트 선택
3. Settings → Domains
4. `costwise.site` 추가
5. 가비아에서 다음 CNAME 레코드 추가:
   - 타입: CNAME
   - 호스트: www
   - 값: cname.vercel-dns.com
   - TTL: 300

### 프론트엔드 환경변수 설정
Vercel 프로젝트 설정에서:
```
VITE_API_URL=https://api.costwise.site
```

## 7. 도메인 구조

설정 완료 후 다음과 같이 작동합니다:
- `https://costwise.site` → Vercel 프론트엔드
- `https://api.costwise.site` → EC2 백엔드 API
- `https://www.costwise.site` → Vercel 프론트엔드 (costwise.site로 리다이렉트)