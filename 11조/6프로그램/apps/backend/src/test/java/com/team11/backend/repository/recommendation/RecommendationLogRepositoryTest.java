package com.team11.backend.repository.recommendation;

import com.team11.backend.domain.recommendation.entity.Recommendation;
import com.team11.backend.domain.recommendation.entity.RecommendationLog;
import com.team11.backend.domain.recommendation.repository.RecommendationLogRepository;
import com.team11.backend.domain.recommendation.repository.RecommendationRepository;
import com.team11.backend.domain.resource.entity.Resource;
import com.team11.backend.domain.resource.entity.type.AwsServiceType;
import com.team11.backend.domain.resource.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RecommendationLogRepositoryTest {

    @Autowired
    private RecommendationLogRepository recommendationLogRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    private Recommendation testRecommendation;

    @BeforeEach
    void setUp() {
        Resource testResource = Resource.builder()
                .userId(1L)
                .awsResourceId("i-testresource-for-log")
                .serviceType(AwsServiceType.EC2)
                .region("ap-northeast-2")
                .isIdle(false)
                .usageRate(60.0F)
                .costUsd(12.0F)
                .lastCheckedAt(LocalDateTime.now())
                .build();
        resourceRepository.save(testResource);

        testRecommendation = Recommendation.builder()
                .resource(testResource)
                .recommendationText("EC2 t3.medium으로 변경")
                .expectedSaving(7.0F)
                .status("pending")
                .build();
        recommendationRepository.save(testRecommendation);
    }

    @DisplayName("RecommendationLog를 저장하고 ID로 조회할 수 있다.")
    @Test
    void saveAndFindById() {
        // Given
        RecommendationLog log = RecommendationLog.builder()
                .recommendation(testRecommendation)
                .userId(1L)
                .action("accept")
                .reason("비용 절감 필요")
                .build();

        // When
        RecommendationLog savedLog = recommendationLogRepository.save(log);
        Optional<RecommendationLog> foundLog = recommendationLogRepository.findById(savedLog.getId());

        // Then
        assertThat(foundLog).isPresent();
        assertThat(foundLog.get().getId()).isEqualTo(savedLog.getId());
        assertThat(foundLog.get().getAction()).isEqualTo("accept");
        assertThat(foundLog.get().getRecommendation().getId()).isEqualTo(testRecommendation.getId());
    }

    @DisplayName("Recommendation 객체로 RecommendationLog 목록을 조회할 수 있다.")
    @Test
    void findByRecommendation() {
        // Given
        RecommendationLog log1 = RecommendationLog.builder()
                .recommendation(testRecommendation)
                .userId(1L)
                .action("accept")
                .reason("비용 절감 필요")
                .build();
        RecommendationLog log2 = RecommendationLog.builder()
                .recommendation(testRecommendation)
                .userId(2L)
                .action("ignore")
                .reason("사용 중")
                .build();

        recommendationLogRepository.save(log1);
        recommendationLogRepository.save(log2);

        // When
        List<RecommendationLog> logs = recommendationLogRepository.findByRecommendation(testRecommendation);

        // Then
        assertThat(logs).hasSize(2);
        assertThat(logs).extracting(RecommendationLog::getRecommendation).containsOnly(testRecommendation);
    }

    @DisplayName("RecommendationLog를 삭제할 수 있다.")
    @Test
    void deleteRecommendationLog() {
        // Given
        RecommendationLog log = RecommendationLog.builder()
                .recommendation(testRecommendation)
                .userId(1L)
                .action("accept")
                .reason("비용 절감 필요")
                .build();
        RecommendationLog savedLog = recommendationLogRepository.save(log);

        // When
        recommendationLogRepository.deleteById(savedLog.getId());
        Optional<RecommendationLog> foundLog = recommendationLogRepository.findById(savedLog.getId());

        // Then
        assertThat(foundLog).isNotPresent();
    }
}