#!/bin/bash

# Session Manager 권한 추가 스크립트

echo "EC2 역할에 SSM 권한 추가 중..."

# SSM 정책 연결
aws iam attach-role-policy \
    --role-name EC2-ECR-Role \
    --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore

echo "✓ SSM 권한 추가 완료"
echo ""
echo "이제 AWS 콘솔에서 Session Manager로 접속할 수 있습니다:"
echo "1. EC2 콘솔에서 인스턴스 선택"
echo "2. 연결 → Session Manager → 연결"