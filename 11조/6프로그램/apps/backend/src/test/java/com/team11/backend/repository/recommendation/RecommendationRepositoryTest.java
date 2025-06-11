package com.team11.backend.repository.recommendation;

import com.team11.backend.domain.recommendation.entity.Recommendation;
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
class RecommendationRepositoryTest {

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    private Resource testResource;

    @BeforeEach
    void setUp() {
        testResource = Resource.builder()
                .userId(1L)
                .awsResourceId("i-testresource1")
                .serviceType(AwsServiceType.EC2)
                .region("ap-northeast-2")
                .isIdle(false)
                .usageRate(50.0F)
                .costUsd(10.0F)
                .lastCheckedAt(LocalDateTime.now())
                .build();
        resourceRepository.save(testResource);
    }

    @DisplayName("Recommendation을 저장하고 ID로 조회할 수 있다.")
    @Test
    void saveAndFindById() {
        // Given
        Recommendation recommendation = Recommendation.builder()
                .resource(testResource)
                .recommendationText("EC2 t3.small로 다운사이징")
                .expectedSaving(5.0F)
                .status("pending")
                .build();

        // When
        Recommendation savedRecommendation = recommendationRepository.save(recommendation);
        Optional<Recommendation> foundRecommendation = recommendationRepository.findById(savedRecommendation.getId());

        // Then
        assertThat(foundRecommendation).isPresent();
        assertThat(foundRecommendation.get().getId()).isEqualTo(savedRecommendation.getId());
        assertThat(foundRecommendation.get().getRecommendationText()).isEqualTo("EC2 t3.small로 다운사이징");
        assertThat(foundRecommendation.get().getResource().getId()).isEqualTo(testResource.getId());
    }

    @DisplayName("Resource 객체로 Recommendation 목록을 조회할 수 있다.")
    @Test
    void findByResource() {
        // Given
        Recommendation recommendation1 = Recommendation.builder()
                .resource(testResource)
                .recommendationText("EC2 t3.small로 다운사이징")
                .expectedSaving(5.0F)
                .status("pending")
                .build();
        Recommendation recommendation2 = Recommendation.builder()
                .resource(testResource)
                .recommendationText("RDS 인스턴스 정지")
                .expectedSaving(10.0F)
                .status("accepted")
                .build();

        recommendationRepository.save(recommendation1);
        recommendationRepository.save(recommendation2);

        // When
        List<Recommendation> recommendations = recommendationRepository.findByResource(testResource);

        // Then
        assertThat(recommendations).hasSize(2);
        assertThat(recommendations).extracting(Recommendation::getResource).containsOnly(testResource);
    }

    @DisplayName("Recommendation을 업데이트할 수 있다.")
    @Test
    void updateRecommendation() {
        // Given
        Recommendation recommendation = Recommendation.builder()
                .resource(testResource)
                .recommendationText("EC2 t3.small로 다운사이징")
                .expectedSaving(5.0F)
                .status("pending")
                .build();
        Recommendation savedRecommendation = recommendationRepository.save(recommendation);

        // When
        savedRecommendation.update("EC2 인스턴스 정지", 8.0F, "completed");
        Recommendation updatedRecommendation = recommendationRepository.save(savedRecommendation); // save를 통해 변경 감지 및 DB 반영

        // Then
        assertThat(updatedRecommendation.getRecommendationText()).isEqualTo("EC2 인스턴스 정지");
        assertThat(updatedRecommendation.getExpectedSaving()).isEqualTo(8.0F);
        assertThat(updatedRecommendation.getStatus()).isEqualTo("completed");
    }

    @DisplayName("Recommendation을 삭제할 수 있다.")
    @Test
    void deleteRecommendation() {
        // Given
        Recommendation recommendation = Recommendation.builder()
                .resource(testResource)
                .recommendationText("EC2 t3.small로 다운사이징")
                .expectedSaving(5.0F)
                .status("pending")
                .build();
        Recommendation savedRecommendation = recommendationRepository.save(recommendation);

        // When
        recommendationRepository.deleteById(savedRecommendation.getId());
        Optional<Recommendation> foundRecommendation = recommendationRepository.findById(savedRecommendation.getId());

        // Then
        assertThat(foundRecommendation).isNotPresent();
    }
}