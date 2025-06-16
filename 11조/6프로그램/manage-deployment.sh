#!/bin/bash

# 배포 관리 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 환경 변수 로드
if [ -f ".env.production" ]; then
    export $(cat .env.production | grep -v '^#' | xargs)
fi

function show_menu() {
    echo -e "${BLUE}===========================================${NC}"
    echo -e "${BLUE}    Team 11 배포 관리 도구${NC}"
    echo -e "${BLUE}===========================================${NC}"
    echo "1. 서비스 상태 확인"
    echo "2. 로그 보기"
    echo "3. 서비스 재시작"
    echo "4. 데이터베이스 백업"
    echo "5. 모니터링 대시보드"
    echo "6. SSH 접속"
    echo "7. 리소스 사용량 확인"
    echo "8. 배포 롤백"
    echo "9. 인프라 삭제 (주의!)"
    echo "0. 종료"
    echo -e "${BLUE}===========================================${NC}"
}

function check_service_status() {
    echo -e "${YELLOW}서비스 상태 확인 중...${NC}"
    
    if [ -f "team11-cloud-cost-key.pem" ] && [ ! -z "$EC2_PUBLIC_IP" ]; then
        ssh -i team11-cloud-cost-key.pem -o StrictHostKeyChecking=no ubuntu@$EC2_PUBLIC_IP << 'EOF'
            echo "Docker 컨테이너 상태:"
            docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
            
            echo -e "\n헬스체크:"
            services=("8761" "8000")
            for port in "${services[@]}"; do
                echo -n "Port $port: "
                if curl -s "http://localhost:$port/actuator/health" | grep -q "UP"; then
                    echo "✅ 정상"
                else
                    echo "❌ 응답 없음"
                fi
            done
EOF
    else
        echo -e "${RED}EC2 접속 정보가 없습니다.${NC}"
    fi
}

function view_logs() {
    echo "로그를 확인할 서비스를 선택하세요:"
    echo "1. 전체 로그"
    echo "2. Eureka"
    echo "3. Gateway"
    echo "4. Auth Service"
    echo "5. User Service"
    echo "6. Backend Service"
    
    read -p "선택: " log_choice
    
    SERVICE_NAME=""
    case $log_choice in
        1) SERVICE_NAME="";;
        2) SERVICE_NAME="eureka-service";;
        3) SERVICE_NAME="gateway-service";;
        4) SERVICE_NAME="auth-service";;
        5) SERVICE_NAME="user-service";;
        6) SERVICE_NAME="backend-service";;
        *) echo "잘못된 선택"; return;;
    esac
    
    if [ -f "team11-cloud-cost-key.pem" ] && [ ! -z "$EC2_PUBLIC_IP" ]; then
        echo -e "${YELLOW}로그 출력 중... (Ctrl+C로 종료)${NC}"
        ssh -i team11-cloud-cost-key.pem -o StrictHostKeyChecking=no ubuntu@$EC2_PUBLIC_IP \
            "cd /home/ubuntu/industry-project-02-11/11조/6프로그램 && docker-compose -f docker-compose.prod.yml logs -f $SERVICE_NAME"
    fi
}

function restart_services() {
    echo -e "${YELLOW}서비스를 재시작합니다...${NC}"
    
    if [ -f "team11-cloud-cost-key.pem" ] && [ ! -z "$EC2_PUBLIC_IP" ]; then
        ssh -i team11-cloud-cost-key.pem -o StrictHostKeyChecking=no ubuntu@$EC2_PUBLIC_IP << 'EOF'
            cd /home/ubuntu/industry-project-02-11/11조/6프로그램
            docker-compose -f docker-compose.prod.yml restart
            echo "✅ 서비스 재시작 완료"
EOF
    fi
}

function backup_database() {
    echo -e "${YELLOW}데이터베이스 백업 중...${NC}"
    
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="backup_${TIMESTAMP}.sql"
    
    if [ ! -z "$DB_URL" ] && [ ! -z "$DB_USERNAME" ] && [ ! -z "$DB_PASSWORD" ]; then
        # URL에서 호스트 추출
        DB_HOST=$(echo $DB_URL | sed -n 's|.*://\([^:/]*\).*|\1|p')
        
        echo "백업 파일: $BACKUP_FILE"
        PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -U $DB_USERNAME -d team11_cloud_cost > $BACKUP_FILE
        
        echo -e "${GREEN}✅ 백업 완료: $BACKUP_FILE${NC}"
    else
        echo -e "${RED}데이터베이스 접속 정보가 없습니다.${NC}"
    fi
}

function monitoring_dashboard() {
    echo -e "${BLUE}모니터링 URL:${NC}"
    if [ ! -z "$EC2_PUBLIC_IP" ]; then
        echo "  - Eureka Dashboard: http://$EC2_PUBLIC_IP:8761"
        echo "  - API Gateway Health: http://$EC2_PUBLIC_IP:8000/actuator/health"
        echo ""
        echo "CloudWatch 대시보드:"
        echo "  https://console.aws.amazon.com/cloudwatch/home?region=ap-northeast-2"
    else
        echo -e "${RED}EC2 IP 정보가 없습니다.${NC}"
    fi
}

function ssh_connect() {
    if [ -f "team11-cloud-cost-key.pem" ] && [ ! -z "$EC2_PUBLIC_IP" ]; then
        echo -e "${YELLOW}EC2 인스턴스에 접속합니다...${NC}"
        ssh -i team11-cloud-cost-key.pem ubuntu@$EC2_PUBLIC_IP
    else
        echo -e "${RED}SSH 접속 정보가 없습니다.${NC}"
    fi
}

function check_resources() {
    echo -e "${YELLOW}AWS 리소스 사용량 확인 중...${NC}"
    
    # EC2 인스턴스
    echo -e "\n${BLUE}EC2 인스턴스:${NC}"
    aws ec2 describe-instances \
        --filters "Name=tag:Name,Values=team11-cloud-cost-server" \
        --query 'Reservations[*].Instances[*].[InstanceId,InstanceType,State.Name,PublicIpAddress]' \
        --output table
    
    # RDS
    echo -e "\n${BLUE}RDS 인스턴스:${NC}"
    aws rds describe-db-instances \
        --db-instance-identifier team11-cloud-cost-db \
        --query 'DBInstances[*].[DBInstanceIdentifier,DBInstanceClass,DBInstanceStatus,AllocatedStorage]' \
        --output table 2>/dev/null || echo "RDS 인스턴스 없음"
    
    # ElastiCache
    echo -e "\n${BLUE}ElastiCache 클러스터:${NC}"
    aws elasticache describe-cache-clusters \
        --cache-cluster-id team11-cloud-cost-redis \
        --query 'CacheClusters[*].[CacheClusterId,CacheNodeType,CacheClusterStatus,NumCacheNodes]' \
        --output table 2>/dev/null || echo "Redis 클러스터 없음"
}

function rollback_deployment() {
    echo -e "${YELLOW}배포 롤백을 시작합니다...${NC}"
    echo "이전 버전의 태그를 입력하세요 (예: v1.0.0):"
    read -p "태그: " TAG
    
    if [ -z "$TAG" ]; then
        echo -e "${RED}태그를 입력해주세요.${NC}"
        return
    fi
    
    # 이미지 태그 변경 및 재배포
    echo "롤백 기능은 구현 예정입니다."
}

function delete_infrastructure() {
    echo -e "${RED}⚠️  경고: 모든 AWS 리소스가 삭제됩니다!${NC}"
    echo -e "${RED}이 작업은 되돌릴 수 없습니다.${NC}"
    read -p "정말로 삭제하시겠습니까? 'DELETE'를 입력하세요: " CONFIRM
    
    if [ "$CONFIRM" = "DELETE" ]; then
        echo -e "${YELLOW}인프라 삭제를 시작합니다...${NC}"
        
        # 스크립트 생성
        cat > delete-all-resources.sh << 'EOF'
#!/bin/bash
REGION="ap-northeast-2"
PROJECT_NAME="team11-cloud-cost"

# EC2 인스턴스 종료
INSTANCE_ID=$(aws ec2 describe-instances --filters "Name=tag:Name,Values=${PROJECT_NAME}-server" --query 'Reservations[0].Instances[0].InstanceId' --output text)
if [ "$INSTANCE_ID" != "None" ]; then
    aws ec2 terminate-instances --instance-ids $INSTANCE_ID
    aws ec2 wait instance-terminated --instance-ids $INSTANCE_ID
fi

# RDS 삭제
aws rds delete-db-instance --db-instance-identifier ${PROJECT_NAME}-db --skip-final-snapshot --delete-automated-backups 2>/dev/null

# ElastiCache 삭제
aws elasticache delete-cache-cluster --cache-cluster-id ${PROJECT_NAME}-redis 2>/dev/null

# 나머지 리소스 정리는 수동으로 진행
echo "VPC, 서브넷, 보안 그룹 등은 AWS 콘솔에서 수동으로 삭제해주세요."
EOF
        chmod +x delete-all-resources.sh
        ./delete-all-resources.sh
    else
        echo -e "${GREEN}삭제가 취소되었습니다.${NC}"
    fi
}

# 메인 루프
while true; do
    show_menu
    read -p "선택: " choice
    
    case $choice in
        1) check_service_status;;
        2) view_logs;;
        3) restart_services;;
        4) backup_database;;
        5) monitoring_dashboard;;
        6) ssh_connect;;
        7) check_resources;;
        8) rollback_deployment;;
        9) delete_infrastructure;;
        0) echo "종료합니다."; exit 0;;
        *) echo -e "${RED}잘못된 선택입니다.${NC}";;
    esac
    
    echo ""
    read -p "계속하려면 Enter를 누르세요..."
done
