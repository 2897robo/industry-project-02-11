#!/bin/bash

# IAM 역할 및 정책 설정 스크립트

set -e

echo "IAM 역할 설정 시작..."

# EC2가 ECR에 접근할 수 있는 역할 생성
cat > ec2-trust-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# IAM 역할 생성
aws iam create-role \
    --role-name EC2-ECR-Access \
    --assume-role-policy-document file://ec2-trust-policy.json \
    --description "Allows EC2 instances to access ECR"

# ECR 읽기 권한 정책 연결
aws iam attach-role-policy \
    --role-name EC2-ECR-Access \
    --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly

# CloudWatch Logs 권한 추가
aws iam attach-role-policy \
    --role-name EC2-ECR-Access \
    --policy-arn arn:aws:iam::aws:policy/CloudWatchLogsFullAccess

# 인스턴스 프로파일 생성
aws iam create-instance-profile --instance-profile-name EC2-ECR-Access

# 역할을 인스턴스 프로파일에 추가
aws iam add-role-to-instance-profile \
    --instance-profile-name EC2-ECR-Access \
    --role-name EC2-ECR-Access

echo "✓ IAM 역할 설정 완료"

# 정리
rm -f ec2-trust-policy.json
