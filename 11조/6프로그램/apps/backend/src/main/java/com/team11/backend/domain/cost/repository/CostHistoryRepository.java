package com.team11.backend.domain.cost.repository;

import com.team11.backend.domain.cost.entity.CostHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CostHistoryRepository extends JpaRepository<CostHistory, Long> {
    
    // 사용자의 특정 기간 비용 이력 조회
    List<CostHistory> findByUserUidAndUsageDateBetweenOrderByUsageDateDesc(
            String userUid, LocalDate startDate, LocalDate endDate);
    
    // 사용자의 특정 AWS 계정의 비용 이력 조회
    List<CostHistory> findByUserUidAndAwsAccountIdAndUsageDateBetweenOrderByUsageDateDesc(
            String userUid, Long awsAccountId, LocalDate startDate, LocalDate endDate);
    
    // 사용자의 서비스별 비용 합계 조회
    @Query("SELECT ch.serviceName, SUM(ch.cost) FROM CostHistory ch " +
           "WHERE ch.userUid = :userUid AND ch.usageDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ch.serviceName ORDER BY SUM(ch.cost) DESC")
    List<Object[]> findServiceCostSummaryByUserUidAndDateRange(
            @Param("userUid") String userUid, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);
    
    // 사용자의 일별 총 비용 조회
    @Query("SELECT ch.usageDate, SUM(ch.cost) FROM CostHistory ch " +
           "WHERE ch.userUid = :userUid AND ch.usageDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ch.usageDate ORDER BY ch.usageDate")
    List<Object[]> findDailyCostByUserUidAndDateRange(
            @Param("userUid") String userUid, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);
    
    // 사용자의 현재 월 총 비용 조회
    @Query("SELECT SUM(ch.cost) FROM CostHistory ch " +
           "WHERE ch.userUid = :userUid AND ch.usageDate >= :monthStart")
    BigDecimal findCurrentMonthTotalCostByUserUid(
            @Param("userUid") String userUid, 
            @Param("monthStart") LocalDate monthStart);
}
