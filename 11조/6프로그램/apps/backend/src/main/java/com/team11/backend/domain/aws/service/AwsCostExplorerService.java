package com.team11.backend.domain.aws.service;

import com.team11.backend.commons.exception.ApplicationException;
import com.team11.backend.commons.exception.payload.ErrorStatus;
import com.team11.backend.domain.aws.dto.AwsAccountDto;
import com.team11.backend.domain.cost.entity.CostHistory;
import com.team11.backend.domain.cost.repository.CostHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsCostExplorerService {

    private final AwsAccountService awsAccountService;
    private final CostHistoryRepository costHistoryRepository;

    @Async
    @Transactional
    public void collectCostDataForAccount(String userUid, Long awsAccountId, LocalDate startDate, LocalDate endDate) {
        log.info("AWS 비용 데이터 수집 시작: userUid={}, accountId={}, period={} ~ {}", 
                userUid, awsAccountId, startDate, endDate);
        
        try {
            AwsAccountDto.Credentials credentials = awsAccountService.getDecryptedCredentials(userUid, awsAccountId);
            
            try (CostExplorerClient costClient = createCostExplorerClient(credentials)) {
                // 일별 비용 조회
                GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
                        .timePeriod(DateInterval.builder()
                                .start(startDate.format(DateTimeFormatter.ISO_DATE))
                                .end(endDate.format(DateTimeFormatter.ISO_DATE))
                                .build())
                        .granularity(Granularity.DAILY)
                        .metrics("UnblendedCost")
                        .groupBy(GroupDefinition.builder()
                                .type(GroupDefinitionType.DIMENSION)
                                .key("SERVICE")
                                .build())
                        .build();

                GetCostAndUsageResponse response = costClient.getCostAndUsage(request);
                
                saveCostData(userUid, awsAccountId, response);
                
                log.info("AWS 비용 데이터 수집 완료: userUid={}, accountId={}", userUid, awsAccountId);
            }
        } catch (Exception e) {
            log.error("AWS 비용 데이터 수집 실패: {}", e.getMessage());
            throw new ApplicationException(
                ErrorStatus.toErrorStatus("AWS 비용 데이터 수집 중 오류가 발생했습니다.", 500, LocalDateTime.now())
            );
        }
    }

    @Transactional
    public void collectMonthlyCostData(String userUid, Long awsAccountId) {
        // 현재 월의 1일부터 오늘까지
        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now().plusDays(1); // Cost Explorer는 end date를 exclusive로 처리
        
        collectCostDataForAccount(userUid, awsAccountId, startDate, endDate);
    }

    private void saveCostData(String userUid, Long awsAccountId, GetCostAndUsageResponse response) {
        List<CostHistory> costHistories = new ArrayList<>();
        
        for (ResultByTime resultByTime : response.resultsByTime()) {
            LocalDate usageDate = LocalDate.parse(resultByTime.timePeriod().start());
            
            for (Group group : resultByTime.groups()) {
                String serviceName = group.keys().get(0);
                String costAmount = group.metrics().get("UnblendedCost").amount();
                
                CostHistory costHistory = CostHistory.builder()
                        .userUid(userUid)
                        .awsAccountId(awsAccountId)
                        .serviceName(serviceName)
                        .cost(new BigDecimal(costAmount))
                        .currency("USD")
                        .usageDate(usageDate)
                        .rawData(group.toString()) // 원본 데이터 저장
                        .build();
                
                costHistories.add(costHistory);
            }
        }
        
        costHistoryRepository.saveAll(costHistories);
        log.info("비용 데이터 {} 건 저장 완료", costHistories.size());
    }

    // 특정 리소스의 비용 조회
    public BigDecimal getResourceCost(String userUid, Long awsAccountId, String resourceId, LocalDate date) {
        try {
            AwsAccountDto.Credentials credentials = awsAccountService.getDecryptedCredentials(userUid, awsAccountId);
            
            try (CostExplorerClient costClient = createCostExplorerClient(credentials)) {
                GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
                        .timePeriod(DateInterval.builder()
                                .start(date.format(DateTimeFormatter.ISO_DATE))
                                .end(date.plusDays(1).format(DateTimeFormatter.ISO_DATE))
                                .build())
                        .granularity(Granularity.DAILY)
                        .metrics("UnblendedCost")
                        .filter(Expression.builder()
                                .dimensions(DimensionValues.builder()
                                        .key(Dimension.RESOURCE_ID)
                                        .values(resourceId)
                                        .build())
                                .build())
                        .build();

                GetCostAndUsageResponse response = costClient.getCostAndUsage(request);
                
                if (!response.resultsByTime().isEmpty() && !response.resultsByTime().get(0).groups().isEmpty()) {
                    String amount = response.resultsByTime().get(0).total().get("UnblendedCost").amount();
                    return new BigDecimal(amount);
                }
                
                return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.error("리소스 비용 조회 실패: resourceId={}, error={}", resourceId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private CostExplorerClient createCostExplorerClient(AwsAccountDto.Credentials credentials) {
        return CostExplorerClient.builder()
                .region(Region.US_EAST_1) // Cost Explorer는 us-east-1만 지원
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey())
                ))
                .build();
    }
}
