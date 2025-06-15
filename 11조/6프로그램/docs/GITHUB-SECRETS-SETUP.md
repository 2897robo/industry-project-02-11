# GitHub Secrets 설정 가이드

## CI/CD를 위한 GitHub Secrets 설정

GitHub Actions에서 AWS 배포를 위해 다음 Secrets를 설정해야 합니다.

### 1. GitHub 리포지토리에서 Secrets 설정

1. GitHub 리포지토리 페이지로 이동
2. Settings → Secrets and variables → Actions 클릭
3. "New repository secret" 버튼 클릭

### 2. 필요한 Secrets 목록

| Secret 이름 | 설명 | 예시 값 |
|------------|------|---------|
| `AWS_ACCESS_KEY_ID` | AWS IAM 사용자의 Access Key ID | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM 사용자의 Secret Access Key | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| `EC2_HOST` | EC2 인스턴스의 Public IP 또는 도메인 | `13.125.xxx.xxx` |
| `EC2_SSH_KEY` | EC2 접속용 SSH Private Key | `-----BEGIN RSA PRIVATE KEY-----...` |
| `CLOUDFRONT_DISTRIBUTION_ID` | CloudFront Distribution ID | `E1PDGXXXXXXXXXXX` |

### 3. AWS IAM 사용자 권한

CI/CD용 IAM 사용자는 다음 권한이 필요합니다:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:PutObjectAcl",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::team11-cloud-cost-frontend/*",
        "arn:aws:s3:::team11-cloud-cost-frontend"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudfront:CreateInvalidation"
      ],
      "Resource": "*"
    }
  ]
}
```

### 4. SSH Key 설정 방법

1. EC2 Key Pair의 Private Key를 복사
2. GitHub Secret에 전체 내용 붙여넣기 (BEGIN/END 라인 포함)

```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA...
...
...전체 키 내용...
...
-----END RSA PRIVATE KEY-----
```

### 5. 보안 주의사항

⚠️ **절대로 다음을 하지 마세요:**
- Secrets를 코드에 직접 작성
- 로그에 Secrets 출력
- Public 리포지토리에 실제 값 커밋

✅ **항상 다음을 확인하세요:**
- IAM 사용자는 최소 권한 원칙 적용
- 정기적으로 Access Key 교체
- 사용하지 않는 Secrets 삭제

### 6. 로컬 개발 환경

로컬에서는 `.env` 파일을 사용합니다:

```bash
# .env 파일 (절대 커밋하지 마세요!)
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
```

`.gitignore`에 다음이 포함되어 있는지 확인:
```
.env
.env.*
!.env.example
!.env.prod.example
```

### 7. 문제 해결

#### Actions에서 AWS 인증 실패
- IAM 사용자 권한 확인
- Secret 이름이 정확한지 확인
- Access Key가 활성 상태인지 확인

#### EC2 SSH 연결 실패
- SSH Key 형식 확인 (개행문자 포함)
- EC2 Security Group에서 GitHub Actions IP 허용
- EC2 인스턴스가 실행 중인지 확인

---
작성일: 2025-01-17
