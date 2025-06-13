package com.team11.backend.domain.cost.service;

import com.team11.backend.domain.cost.dto.CostHistoryDto;
import com.team11.backend.domain.cost.repository.CostHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CostHistoryService {

    private final CostHistoryRepository costHistoryRepository;

    // 사용자의 특정 기간 비용 이력 조회
    public List<CostHistoryDto.Response> getCostHistory(String userUid, LocalDate startDate, LocalDate endDate) {
        return costHistoryRepository.findByUserUidAndUsageDateBetweenOrderByUsageDateDesc(userUid, startDate, endDate)
                .stream()
                .map(CostHistoryDto.Response::from)
                .collect(Collectors.toList());
    }

    // 사용자의 특정 AWS 계정의 비용 이력 조회
    public List<CostHistoryDto.Response> getCostHistoryByAwsAccount(String userUid, Long awsAccountId, 
                                                                     LocalDate startDate, LocalDate endDate) {
        return costHistoryRepository.findByUserUidAndAwsAccountIdAndUsageDateBetweenOrderByUsageDateDesc(
                userUid, awsAccountId, startDate, endDate)
                .stream()
                .map(CostHistoryDto.Response::from)
                .collect(Collectors.toList());
    }

    // 서비스별 비용 요약 조회
    public List<CostHistoryDto.ServiceCostSummary> getServiceCostSummary(String userUid, 
                                                                          LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = costHistoryRepository.findServiceCostSummaryByUserUidAndDateRange(
                userUid, startDate, endDate);
        
        return results.stream()
                .map(result -> CostHistoryDto.ServiceCostSummary.builder()
                        .serviceName((String) result[0])
                        .totalCost((BigDecimal) result[1])
                        .currency("USD")
                        .build())
                .collect(Collectors.toList());
    }

    // 일별 비용 추이 조회
    public CostHistoryDto.CostTrend getDailyCostTrend(String userUid, LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = costHistoryRepository.findDailyCostByUserUidAndDateRange(userUid, startDate, endDate);
        
        List<CostHistoryDto.DailyCostSummary> dailyCosts = results.stream()
                .map(result -> CostHistoryDto.DailyCostSummary.builder()
                        .date((LocalDate) result[0])
                        .totalCost((BigDecimal) result[1])
                        .currency("USD")
                        .build())
                .collect(Collectors.toList());
        
        // 총 비용 계산
        BigDecimal totalCost = dailyCosts.stream()
                .map(CostHistoryDto.DailyCostSummary::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 일 평균 비용 계산
        BigDecimal averageDailyCost = BigDecimal.ZERO;
        if (!dailyCosts.isEmpty()) {
            averageDailyCost = totalCost.divide(
                    new BigDecimal(dailyCosts.size()), 2, RoundingMode.HALF_UP);
        }
        
        // 예상 월 비용 계산 (현재까지의 일 평균 * 30)
        BigDecimal projectedMonthlyCost = averageDailyCost.multiply(new BigDecimal(30));
        
        return CostHistoryDto.CostTrend.builder()
                .dailyCosts(dailyCosts)
                .totalCost(totalCost)
                .averageDailyCost(averageDailyCost)
                .projectedMonthlyCost(projectedMonthlyCost)
                .currency("USD")
                .build();
    }

    // 현재 월 비용 요약 조회
    public CostHistoryDto.MonthlyCostSummary getCurrentMonthSummary(String userUid) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = LocalDate.now();
        
        // 현재 월 총 비용
        BigDecimal totalCost = costHistoryRepository.findCurrentMonthTotalCostByUserUid(userUid, monthStart);
        if (totalCost == null) {
            totalCost = BigDecimal.ZERO;
        }
        
        // 일 평균 계산
        int daysInMonth = monthEnd.getDayOfMonth();
        BigDecimal dailyAverage = totalCost.divide(new BigDecimal(daysInMonth), 2, RoundingMode.HALF_UP);
        
        // Top 5 서비스
        List<CostHistoryDto.ServiceCostSummary> topServices = getServiceCostSummary(userUid, monthStart, monthEnd)
                .stream()
                .limit(5)
                .collect(Collectors.toList());
        
        return CostHistoryDto.MonthlyCostSummary.builder()
                .month(currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                .totalCost(totalCost)
                .dailyAverage(dailyAverage)
                .currency("USD")
                .topServices(topServices)
                .build();
    }

    // 지난 N개월 비용 추이
    public List<CostHistoryDto.MonthlyCostSummary> getMonthlyTrend(String userUid, int months) {
        List<CostHistoryDto.MonthlyCostSummary> monthlyTrends = new ArrayList<>();
        
        for (int i = 0; i < months; i++) {
            YearMonth targetMonth = YearMonth.now().minusMonths(i);
            LocalDate monthStart = targetMonth.atDay(1);
            LocalDate monthEnd = targetMonth.atEndOfMonth();
            
            // 현재 월인 경우 오늘까지만
            if (i == 0) {
                monthEnd = LocalDate.now();
            }
            
            List<Object[]> dailyCosts = costHistoryRepository.findDailyCostByUserUidAndDateRange(
                    userUid, monthStart, monthEnd);
            
            BigDecimal monthTotal = dailyCosts.stream()
                    .map(result -> (BigDecimal) result[1])
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            int daysInPeriod = dailyCosts.size();
            BigDecimal dailyAverage = BigDecimal.ZERO;
            if (daysInPeriod > 0) {
                dailyAverage = monthTotal.divide(new BigDecimal(daysInPeriod), 2, RoundingMode.HALF_UP);
            }
            
            monthlyTrends.add(CostHistoryDto.MonthlyCostSummary.builder()
                    .month(targetMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .totalCost(monthTotal)
                    .dailyAverage(dailyAverage)
                    .currency("USD")
                    .build());
        }
        
        return monthlyTrends;
    }
}
