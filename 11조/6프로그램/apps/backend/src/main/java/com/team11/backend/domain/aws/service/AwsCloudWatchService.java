package com.team11.backend.domain.aws.service;

import com.team11.backend.domain.aws.dto.AwsAccountDto;
import com.team11.backend.domain.resource.entity.Resource;
import com.team11.backend.domain.resource.entity.type.AwsServiceType;
import com.team11.backend.domain.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsCloudWatchService {

    private final AwsAccountService awsAccountService;
    private final ResourceRepository resourceRepository;

    @Transactional
    public void updateResourceMetrics(String userUid, Long awsAccountId) {
        log.info("리소스 메트릭 업데이트 시작: userUid={}, accountId={}", userUid, awsAccountId);
        
        try {
            AwsAccountDto.Credentials credentials = awsAccountService.getDecryptedCredentials(userUid, awsAccountId);
            List<Resource> resources = resourceRepository.findByUserUid(userUid);
            
            try (CloudWatchClient cloudWatchClient = createCloudWatchClient(credentials)) {
                for (Resource resource : resources) {
                    updateResourceMetric(resource, cloudWatchClient);
                }
            }
            
            log.info("리소스 메트릭 업데이트 완료: userUid={}, accountId={}", userUid, awsAccountId);
        } catch (Exception e) {
            log.error("리소스 메트릭 업데이트 실패: {}", e.getMessage());
        }
    }

    private void updateResourceMetric(Resource resource, CloudWatchClient cloudWatchClient) {
        try {
            Float usageRate = null;
            boolean isIdle = false;
            
            switch (resource.getServiceType()) {
                case EC2:
                    usageRate = getEc2CpuUtilization(resource.getAwsResourceId(), cloudWatchClient);
                    isIdle = isEc2Idle(usageRate);
                    break;
                case RDS:
                    usageRate = getRdsCpuUtilization(resource.getAwsResourceId(), cloudWatchClient);
                    isIdle = isRdsIdle(usageRate);
                    break;
                case Lambda:
                    usageRate = getLambdaInvocations(resource.getAwsResourceId(), cloudWatchClient);
                    isIdle = isLambdaIdle(usageRate);
                    break;
                default:
                    // 다른 서비스 타입은 별도 로직 필요
                    break;
            }
            
            if (usageRate != null) {
                resource.update(
                    resource.getServiceType(),
                    resource.getRegion(),
                    isIdle,
                    usageRate,
                    resource.getCostUsd(),
                    LocalDateTime.now()
                );
                resourceRepository.save(resource);
            }
        } catch (Exception e) {
            log.error("리소스 메트릭 업데이트 실패: resourceId={}, error={}", 
                     resource.getAwsResourceId(), e.getMessage());
        }
    }

    private Float getEc2CpuUtilization(String instanceId, CloudWatchClient cloudWatchClient) {
        try {
            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(7, ChronoUnit.DAYS); // 최근 7일
            
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/EC2")
                    .metricName("CPUUtilization")
                    .dimensions(Dimension.builder()
                            .name("InstanceId")
                            .value(instanceId)
                            .build())
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(3600) // 1시간 단위
                    .statistics(Statistic.AVERAGE)
                    .build();
            
            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
            
            if (!response.datapoints().isEmpty()) {
                // 전체 기간의 평균 계산
                double avgCpu = response.datapoints().stream()
                        .mapToDouble(Datapoint::average)
                        .average()
                        .orElse(0.0);
                
                return (float) avgCpu;
            }
        } catch (Exception e) {
            log.error("EC2 CPU 사용률 조회 실패: instanceId={}, error={}", instanceId, e.getMessage());
        }
        
        return null;
    }

    private Float getRdsCpuUtilization(String dbInstanceId, CloudWatchClient cloudWatchClient) {
        try {
            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(7, ChronoUnit.DAYS);
            
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/RDS")
                    .metricName("CPUUtilization")
                    .dimensions(Dimension.builder()
                            .name("DBInstanceIdentifier")
                            .value(dbInstanceId)
                            .build())
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(3600)
                    .statistics(Statistic.AVERAGE)
                    .build();
            
            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
            
            if (!response.datapoints().isEmpty()) {
                double avgCpu = response.datapoints().stream()
                        .mapToDouble(Datapoint::average)
                        .average()
                        .orElse(0.0);
                
                return (float) avgCpu;
            }
        } catch (Exception e) {
            log.error("RDS CPU 사용률 조회 실패: dbInstanceId={}, error={}", dbInstanceId, e.getMessage());
        }
        
        return null;
    }

    private Float getLambdaInvocations(String functionName, CloudWatchClient cloudWatchClient) {
        try {
            // Lambda ARN에서 함수 이름 추출
            String funcName = extractLambdaFunctionName(functionName);
            
            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(7, ChronoUnit.DAYS);
            
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/Lambda")
                    .metricName("Invocations")
                    .dimensions(Dimension.builder()
                            .name("FunctionName")
                            .value(funcName)
                            .build())
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(86400) // 1일 단위
                    .statistics(Statistic.SUM)
                    .build();
            
            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
            
            if (!response.datapoints().isEmpty()) {
                // 일일 평균 호출 수
                double avgInvocations = response.datapoints().stream()
                        .mapToDouble(Datapoint::sum)
                        .average()
                        .orElse(0.0);
                
                return (float) avgInvocations;
            }
        } catch (Exception e) {
            log.error("Lambda 호출 수 조회 실패: functionName={}, error={}", functionName, e.getMessage());
        }
        
        return null;
    }

    // 유휴 판단 메서드들
    private boolean isEc2Idle(Float cpuUtilization) {
        // CPU 사용률이 10% 미만이면 유휴로 판단
        return cpuUtilization != null && cpuUtilization < 10.0f;
    }

    private boolean isRdsIdle(Float cpuUtilization) {
        // CPU 사용률이 20% 미만이면 유휴로 판단
        return cpuUtilization != null && cpuUtilization < 20.0f;
    }

    private boolean isLambdaIdle(Float invocations) {
        // 일일 호출 수가 10회 미만이면 유휴로 판단
        return invocations != null && invocations < 10.0f;
    }

    private String extractLambdaFunctionName(String functionArn) {
        // ARN 형식: arn:aws:lambda:region:account-id:function:function-name
        String[] parts = functionArn.split(":");
        return parts.length > 6 ? parts[6] : functionArn;
    }

    private CloudWatchClient createCloudWatchClient(AwsAccountDto.Credentials credentials) {
        return CloudWatchClient.builder()
                .region(Region.of(credentials.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey())
                ))
                .build();
    }

    // 특정 리소스의 상세 메트릭 조회
    public GetMetricStatisticsResponse getResourceDetailedMetrics(String userUid, Long awsAccountId, 
                                                 String resourceId, AwsServiceType serviceType,
                                                 LocalDateTime startTime, LocalDateTime endTime) {
        try {
            AwsAccountDto.Credentials credentials = awsAccountService.getDecryptedCredentials(userUid, awsAccountId);
            
            try (CloudWatchClient cloudWatchClient = createCloudWatchClient(credentials)) {
                String namespace;
                String metricName;
                Dimension dimension;
                
                switch (serviceType) {
                    case EC2:
                        namespace = "AWS/EC2";
                        metricName = "CPUUtilization";
                        dimension = Dimension.builder()
                                .name("InstanceId")
                                .value(resourceId)
                                .build();
                        break;
                    case RDS:
                        namespace = "AWS/RDS";
                        metricName = "CPUUtilization";
                        dimension = Dimension.builder()
                                .name("DBInstanceIdentifier")
                                .value(resourceId)
                                .build();
                        break;
                    default:
                        return null;
                }
                
                GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                        .namespace(namespace)
                        .metricName(metricName)
                        .dimensions(dimension)
                        .startTime(startTime.toInstant(ZoneOffset.UTC))
                        .endTime(endTime.toInstant(ZoneOffset.UTC))
                        .period(300) // 5분 단위
                        .statistics(Statistic.AVERAGE, Statistic.MAXIMUM, Statistic.MINIMUM)
                        .build();
                
                GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
                
                return response;
            }
        } catch (Exception e) {
            log.error("리소스 상세 메트릭 조회 실패: resourceId={}, error={}", resourceId, e.getMessage());
            return null;
        }
    }
}
