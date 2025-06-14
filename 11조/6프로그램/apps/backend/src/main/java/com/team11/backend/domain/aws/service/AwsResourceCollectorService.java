package com.team11.backend.domain.aws.service;

import com.team11.backend.commons.exception.ApplicationException;
import com.team11.backend.commons.exception.payload.ErrorStatus;
import com.team11.backend.domain.aws.dto.AwsAccountDto;
import com.team11.backend.domain.resource.entity.Resource;
import com.team11.backend.domain.resource.entity.type.AwsServiceType;
import com.team11.backend.domain.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsResourceCollectorService {

    private final AwsAccountService awsAccountService;
    private final ResourceRepository resourceRepository;

    @Async
    @Transactional
    public void collectResourcesForAccount(String userUid, Long awsAccountId) {
        log.info("AWS 리소스 수집 시작: userUid={}, accountId={}", userUid, awsAccountId);
        
        try {
            AwsAccountDto.Credentials credentials = awsAccountService.getDecryptedCredentials(userUid, awsAccountId);
            
            // 각 서비스별 리소스 수집
            collectEc2Resources(userUid, awsAccountId, credentials);
            collectRdsResources(userUid, awsAccountId, credentials);
            collectS3Resources(userUid, awsAccountId, credentials);
            collectLambdaResources(userUid, awsAccountId, credentials);
            
            log.info("AWS 리소스 수집 완료: userUid={}, accountId={}", userUid, awsAccountId);
        } catch (Exception e) {
            log.error("AWS 리소스 수집 실패: {}", e.getMessage());
            throw new ApplicationException(
                ErrorStatus.toErrorStatus("AWS 리소스 수집 중 오류가 발생했습니다.", 500, LocalDateTime.now())
            );
        }
    }

    private void collectEc2Resources(String userUid, Long awsAccountId, AwsAccountDto.Credentials credentials) {
        try (Ec2Client ec2Client = createEc2Client(credentials)) {
            DescribeInstancesResponse response = ec2Client.describeInstances();
            
            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    saveOrUpdateResource(
                        userUid,
                        instance.instanceId(),
                        AwsServiceType.EC2,
                        credentials.getRegion(),
                        instance.state().name() == InstanceStateName.STOPPED,
                        null, // CPU 사용률은 CloudWatch에서 별도로 가져와야 함
                        null  // 비용은 Cost Explorer에서 별도로 가져와야 함
                    );
                }
            }
        }
    }

    private void collectRdsResources(String userUid, Long awsAccountId, AwsAccountDto.Credentials credentials) {
        try (RdsClient rdsClient = createRdsClient(credentials)) {
            DescribeDbInstancesResponse response = rdsClient.describeDBInstances();
            
            for (DBInstance dbInstance : response.dbInstances()) {
                saveOrUpdateResource(
                    userUid,
                    dbInstance.dbInstanceIdentifier(),
                    AwsServiceType.RDS,
                    credentials.getRegion(),
                    !dbInstance.dbInstanceStatus().equals("available"),
                    null,
                    null
                );
            }
        }
    }

    private void collectS3Resources(String userUid, Long awsAccountId, AwsAccountDto.Credentials credentials) {
        try (S3Client s3Client = createS3Client(credentials)) {
            ListBucketsResponse response = s3Client.listBuckets();
            
            for (Bucket bucket : response.buckets()) {
                saveOrUpdateResource(
                    userUid,
                    bucket.name(),
                    AwsServiceType.S3,
                    "global", // S3는 글로벌 서비스
                    false,    // S3 유휴 판단은 별도 로직 필요
                    null,
                    null
                );
            }
        }
    }

    private void collectLambdaResources(String userUid, Long awsAccountId, AwsAccountDto.Credentials credentials) {
        try (LambdaClient lambdaClient = createLambdaClient(credentials)) {
            ListFunctionsResponse response = lambdaClient.listFunctions();
            
            for (FunctionConfiguration function : response.functions()) {
                saveOrUpdateResource(
                    userUid,
                    function.functionArn(),
                    AwsServiceType.Lambda,
                    credentials.getRegion(),
                    false, // Lambda 유휴 판단은 호출 빈도로 별도 체크 필요
                    null,
                    null
                );
            }
        }
    }

    private void saveOrUpdateResource(String userUid, String awsResourceId, AwsServiceType serviceType,
                                     String region, Boolean isIdle, Float usageRate, Float costUsd) {
        Optional<Resource> existingResource = resourceRepository.findByUserUidAndAwsResourceId(userUid, awsResourceId);
        
        if (existingResource.isPresent()) {
            // 기존 리소스 업데이트
            Resource resource = existingResource.get();
            resource.update(serviceType, region, isIdle, usageRate, costUsd, LocalDateTime.now());
        } else {
            // 새 리소스 생성
            Resource newResource = Resource.builder()
                    .userUid(userUid)
                    .awsResourceId(awsResourceId)
                    .serviceType(serviceType)
                    .region(region)
                    .isIdle(isIdle)
                    .usageRate(usageRate)
                    .costUsd(costUsd)
                    .lastCheckedAt(LocalDateTime.now())
                    .build();
            resourceRepository.save(newResource);
        }
    }

    // AWS 클라이언트 생성 메서드들
    private Ec2Client createEc2Client(AwsAccountDto.Credentials credentials) {
        return Ec2Client.builder()
                .region(Region.of(credentials.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey())
                ))
                .build();
    }

    private RdsClient createRdsClient(AwsAccountDto.Credentials credentials) {
        return RdsClient.builder()
                .region(Region.of(credentials.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey())
                ))
                .build();
    }

    private S3Client createS3Client(AwsAccountDto.Credentials credentials) {
        return S3Client.builder()
                .region(Region.of(credentials.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey())
                ))
                .build();
    }

    private LambdaClient createLambdaClient(AwsAccountDto.Credentials credentials) {
        return LambdaClient.builder()
                .region(Region.of(credentials.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey())
                ))
                .build();
    }
}
