package com.team11.backend.domain.recommendation.service;

import com.team11.backend.domain.config.entity.Config;
import com.team11.backend.domain.config.repository.ConfigRepository;
import com.team11.backend.domain.recommendation.entity.Recommendation;
import com.team11.backend.domain.recommendation.repository.RecommendationRepository;
import com.team11.backend.domain.resource.entity.Resource;
import com.team11.backend.domain.resource.entity.type.AwsServiceType;
import com.team11.backend.domain.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostOptimizationService {

    private final ResourceRepository resourceRepository;
    private final RecommendationRepository recommendationRepository;
    private final ConfigRepository configRepository;

    @Transactional
    public List<Recommendation> generateRecommendations(String userUid) {
        log.info("비용 최적화 추천 생성 시작: userUid={}", userUid);
        
        List<Recommendation> recommendations = new ArrayList<>();
        
        // 사용자 설정 조회
        Optional<Config> userConfig = configRepository.findByUserUid(userUid);
        float idleThreshold = userConfig.map(Config::getIdleThreshold).orElse(20.0f);
        
        // 사용자의 모든 리소스 조회
        List<Resource> resources = resourceRepository.findByUserUid(userUid);
        
        for (Resource resource : resources) {
            List<Recommendation> resourceRecommendations = analyzeResource(resource, idleThreshold);
            recommendations.addAll(resourceRecommendations);
        }
        
        // 생성된 추천 저장
        recommendationRepository.saveAll(recommendations);
        
        log.info("비용 최적화 추천 생성 완료: userUid={}, 추천 수={}", userUid, recommendations.size());
        
        return recommendations;
    }

    private List<Recommendation> analyzeResource(Resource resource, float idleThreshold) {
        List<Recommendation> recommendations = new ArrayList<>();
        
        switch (resource.getServiceType()) {
            case EC2:
                recommendations.addAll(analyzeEc2Instance(resource, idleThreshold));
                break;
            case RDS:
                recommendations.addAll(analyzeRdsInstance(resource, idleThreshold));
                break;
            case Lambda:
                recommendations.addAll(analyzeLambdaFunction(resource));
                break;
            case S3:
                recommendations.addAll(analyzeS3Bucket(resource));
                break;
            default:
                break;
        }
        
        return recommendations;
    }

    private List<Recommendation> analyzeEc2Instance(Resource resource, float idleThreshold) {
        List<Recommendation> recommendations = new ArrayList<>();
        
        // 유휴 인스턴스 확인
        if (resource.getUsageRate() != null && resource.getUsageRate() < idleThreshold) {
            Float monthlyCost = resource.getCostUsd() != null ? resource.getCostUsd() * 30 : 0;
            
            if (resource.getUsageRate() < 5.0f) {
                // 매우 낮은 사용률 - 종료 권장
                recommendations.add(Recommendation.builder()
                        .resource(resource)
                        .recommendationText(String.format(
                            "EC2 인스턴스 '%s'의 CPU 사용률이 %.1f%%로 매우 낮습니다. " +
                            "인스턴스를 종료하는 것을 권장합니다.",
                            resource.getAwsResourceId(), resource.getUsageRate()))
                        .expectedSaving(monthlyCost)
                        .status("pending")
                        .build());
            } else {
                // 낮은 사용률 - 다운사이징 권장
                Float expectedSaving = monthlyCost * 0.3f; // 대략 30% 절감 예상
                recommendations.add(Recommendation.builder()
                        .resource(resource)
                        .recommendationText(String.format(
                            "EC2 인스턴스 '%s'의 CPU 사용률이 %.1f%%로 낮습니다. " +
                            "더 작은 인스턴스 타입으로 변경을 권장합니다.",
                            resource.getAwsResourceId(), resource.getUsageRate()))
                        .expectedSaving(expectedSaving)
                        .status("pending")
                        .build());
            }
        }
        
        // Reserved Instance 추천 (사용률이 높고 항상 실행 중인 경우)
        if (resource.getUsageRate() != null && resource.getUsageRate() > 70.0f) {
            Float monthlyCost = resource.getCostUsd() != null ? resource.getCostUsd() * 30 : 0;
            Float expectedSaving = monthlyCost * 0.4f; // RI로 약 40% 절감 가능
            
            recommendations.add(Recommendation.builder()
                    .resource(resource)
                    .recommendationText(String.format(
                        "EC2 인스턴스 '%s'는 높은 사용률(%.1f%%)을 보입니다. " +
                        "Reserved Instance 구매를 통해 비용을 절감할 수 있습니다.",
                        resource.getAwsResourceId(), resource.getUsageRate()))
                    .expectedSaving(expectedSaving)
                    .status("pending")
                    .build());
        }
        
        return recommendations;
    }

    private List<Recommendation> analyzeRdsInstance(Resource resource, float idleThreshold) {
        List<Recommendation> recommendations = new ArrayList<>();
        
        // RDS 유휴 인스턴스 확인
        if (resource.getUsageRate() != null && resource.getUsageRate() < idleThreshold) {
            Float monthlyCost = resource.getCostUsd() != null ? resource.getCostUsd() * 30 : 0;
            
            if (resource.getUsageRate() < 10.0f) {
                // 매우 낮은 사용률
                recommendations.add(Recommendation.builder()
                        .resource(resource)
                        .recommendationText(String.format(
                            "RDS 인스턴스 '%s'의 CPU 사용률이 %.1f%%로 매우 낮습니다. " +
                            "개발/테스트 환경인 경우 필요시에만 실행하거나, " +
                            "Aurora Serverless로 전환을 고려해보세요.",
                            resource.getAwsResourceId(), resource.getUsageRate()))
                        .expectedSaving(monthlyCost * 0.7f)
                        .status("pending")
                        .build());
            } else {
                // 낮은 사용률 - 다운사이징
                recommendations.add(Recommendation.builder()
                        .resource(resource)
                        .recommendationText(String.format(
                            "RDS 인스턴스 '%s'의 사용률이 낮습니다(%.1f%%). " +
                            "더 작은 인스턴스 클래스로 변경을 권장합니다.",
                            resource.getAwsResourceId(), resource.getUsageRate()))
                        .expectedSaving(monthlyCost * 0.3f)
                        .status("pending")
                        .build());
            }
        }
        
        return recommendations;
    }

    private List<Recommendation> analyzeLambdaFunction(Resource resource) {
        List<Recommendation> recommendations = new ArrayList<>();
        
        // Lambda 함수 호출 빈도 확인
        if (resource.getUsageRate() != null && resource.getUsageRate() < 1.0f) {
            // 일일 호출 횟수가 1회 미만
            recommendations.add(Recommendation.builder()
                    .resource(resource)
                    .recommendationText(String.format(
                        "Lambda 함수 '%s'가 거의 사용되지 않습니다(일일 %.1f회 호출). " +
                        "함수 삭제를 고려해보세요.",
                        resource.getAwsResourceId(), resource.getUsageRate()))
                    .expectedSaving(1.0f) // Lambda는 사용량 기반이므로 작은 값
                    .status("pending")
                    .build());
        }
        
        return recommendations;
    }

    private List<Recommendation> analyzeS3Bucket(Resource resource) {
        List<Recommendation> recommendations = new ArrayList<>();
        
        // S3는 접근 패턴 분석이 필요하므로 기본적인 추천만 제공
        if (resource.getCostUsd() != null && resource.getCostUsd() > 10.0f) {
            recommendations.add(Recommendation.builder()
                    .resource(resource)
                    .recommendationText(String.format(
                        "S3 버킷 '%s'의 비용이 높습니다($%.2f/일). " +
                        "라이프사이클 정책을 설정하여 오래된 객체를 Glacier로 이동하거나 " +
                        "삭제하는 것을 권장합니다.",
                        resource.getAwsResourceId(), resource.getCostUsd()))
                    .expectedSaving(resource.getCostUsd() * 30 * 0.5f) // 50% 절감 가능
                    .status("pending")
                    .build());
        }
        
        return recommendations;
    }

    // 특정 리소스에 대한 추천 재생성
    @Transactional
    public List<Recommendation> regenerateRecommendationsForResource(String userUid, Long resourceId) {
        Optional<Resource> resourceOpt = resourceRepository.findByIdAndUserUid(resourceId, userUid);
        
        if (resourceOpt.isEmpty()) {
            return new ArrayList<>();
        }
        
        Resource resource = resourceOpt.get();
        
        // 기존 추천 삭제
        List<Recommendation> existingRecommendations = recommendationRepository.findByResourceId(resourceId);
        recommendationRepository.deleteAll(existingRecommendations);
        
        // 새 추천 생성
        Optional<Config> userConfig = configRepository.findByUserUid(userUid);
        float idleThreshold = userConfig.map(Config::getIdleThreshold).orElse(20.0f);
        
        List<Recommendation> newRecommendations = analyzeResource(resource, idleThreshold);
        return recommendationRepository.saveAll(newRecommendations);
    }
}
