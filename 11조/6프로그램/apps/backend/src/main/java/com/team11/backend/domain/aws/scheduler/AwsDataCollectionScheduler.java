package com.team11.backend.domain.aws.scheduler;

import com.team11.backend.domain.aws.entity.AwsAccount;
import com.team11.backend.domain.aws.repository.AwsAccountRepository;
import com.team11.backend.domain.aws.service.AwsCloudWatchService;
import com.team11.backend.domain.aws.service.AwsCostExplorerService;
import com.team11.backend.domain.aws.service.AwsResourceCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwsDataCollectionScheduler {

    private final AwsAccountRepository awsAccountRepository;
    private final AwsResourceCollectorService resourceCollectorService;
    private final AwsCostExplorerService costExplorerService;
    private final AwsCloudWatchService cloudWatchService;

    // 매일 새벽 2시에 리소스 정보 수집
    @Scheduled(cron = "0 0 2 * * *")
    public void collectResources() {
        log.info("리소스 수집 스케줄러 시작");
        
        List<AwsAccount> activeAccounts = awsAccountRepository.findAll().stream()
                .filter(AwsAccount::getIsActive)
                .toList();
        
        for (AwsAccount account : activeAccounts) {
            try {
                resourceCollectorService.collectResourcesForAccount(
                    account.getUserUid(), 
                    account.getId()
                );
                Thread.sleep(1000); // API 제한을 위한 딜레이
            } catch (Exception e) {
                log.error("리소스 수집 실패: accountId={}, error={}", account.getId(), e.getMessage());
            }
        }
        
        log.info("리소스 수집 스케줄러 완료");
    }

    // 매일 새벽 3시에 비용 데이터 수집
    @Scheduled(cron = "0 0 3 * * *")
    public void collectCostData() {
        log.info("비용 데이터 수집 스케줄러 시작");
        
        List<AwsAccount> activeAccounts = awsAccountRepository.findAll().stream()
                .filter(AwsAccount::getIsActive)
                .toList();
        
        // 어제 날짜의 비용 데이터 수집
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        for (AwsAccount account : activeAccounts) {
            try {
                costExplorerService.collectCostDataForAccount(
                    account.getUserUid(), 
                    account.getId(),
                    yesterday,
                    LocalDate.now()
                );
                Thread.sleep(1000); // API 제한을 위한 딜레이
            } catch (Exception e) {
                log.error("비용 데이터 수집 실패: accountId={}, error={}", account.getId(), e.getMessage());
            }
        }
        
        log.info("비용 데이터 수집 스케줄러 완료");
    }

    // 매시간 리소스 메트릭(CPU 사용률 등) 업데이트
    @Scheduled(cron = "0 0 * * * *")
    public void updateResourceMetrics() {
        log.info("리소스 메트릭 업데이트 스케줄러 시작");
        
        List<AwsAccount> activeAccounts = awsAccountRepository.findAll().stream()
                .filter(AwsAccount::getIsActive)
                .toList();
        
        for (AwsAccount account : activeAccounts) {
            try {
                cloudWatchService.updateResourceMetrics(
                    account.getUserUid(), 
                    account.getId()
                );
                Thread.sleep(1000); // API 제한을 위한 딜레이
            } catch (Exception e) {
                log.error("리소스 메트릭 업데이트 실패: accountId={}, error={}", account.getId(), e.getMessage());
            }
        }
        
        log.info("리소스 메트릭 업데이트 스케줄러 완료");
    }

    // 매월 1일 새벽 4시에 전월 비용 데이터 전체 수집
    @Scheduled(cron = "0 0 4 1 * *")
    public void collectMonthlyFullCostData() {
        log.info("월간 비용 데이터 전체 수집 스케줄러 시작");
        
        List<AwsAccount> activeAccounts = awsAccountRepository.findAll().stream()
                .filter(AwsAccount::getIsActive)
                .toList();
        
        // 전월 1일부터 말일까지
        LocalDate lastMonthStart = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate lastMonthEnd = LocalDate.now().withDayOfMonth(1);
        
        for (AwsAccount account : activeAccounts) {
            try {
                costExplorerService.collectCostDataForAccount(
                    account.getUserUid(), 
                    account.getId(),
                    lastMonthStart,
                    lastMonthEnd
                );
                Thread.sleep(2000); // 큰 데이터이므로 더 긴 딜레이
            } catch (Exception e) {
                log.error("월간 비용 데이터 수집 실패: accountId={}, error={}", account.getId(), e.getMessage());
            }
        }
        
        log.info("월간 비용 데이터 전체 수집 스케줄러 완료");
    }
}
