package com.team11.backend.domain.cost.dto;

import com.team11.backend.domain.cost.entity.CostHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CostHistoryDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String userUid;
        private Long awsAccountId;
        private String serviceName;
        private String resourceType;
        private BigDecimal cost;
        private String currency;
        private LocalDate usageDate;
        private LocalDateTime createdAt;

        public static Response from(CostHistory entity) {
            return Response.builder()
                    .id(entity.getId())
                    .userUid(entity.getUserUid())
                    .awsAccountId(entity.getAwsAccountId())
                    .serviceName(entity.getServiceName())
                    .resourceType(entity.getResourceType())
                    .cost(entity.getCost())
                    .currency(entity.getCurrency())
                    .usageDate(entity.getUsageDate())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceCostSummary {
        private String serviceName;
        private BigDecimal totalCost;
        private String currency;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyCostSummary {
        private LocalDate date;
        private BigDecimal totalCost;
        private String currency;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyCostSummary {
        private String month; // YYYY-MM format
        private BigDecimal totalCost;
        private BigDecimal dailyAverage;
        private String currency;
        private List<ServiceCostSummary> topServices;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CostTrend {
        private List<DailyCostSummary> dailyCosts;
        private BigDecimal totalCost;
        private BigDecimal averageDailyCost;
        private BigDecimal projectedMonthlyCost; // 예상 월 비용
        private String currency;
    }
}
