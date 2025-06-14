//package com.team11.backend.service.recommendation;
//
//import com.team11.backend.domain.recommendation.entity.Recommendation;
//import com.team11.backend.domain.recommendation.entity.RecommendationLog;
//import com.team11.backend.domain.recommendation.dto.RecommendationLogDto;
//import com.team11.backend.domain.recommendation.repository.RecommendationLogRepository;
//import com.team11.backend.domain.recommendation.repository.RecommendationRepository;
//import com.team11.backend.domain.recommendation.service.RecommendationLogService;
//import com.team11.backend.domain.resource.entity.Resource;
//import com.team11.backend.domain.resource.entity.type.AwsServiceType;
//import com.team11.backend.commons.exception.ApplicationException;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class RecommendationLogServiceTest {
//
//    @Mock
//    private RecommendationLogRepository recommendationLogRepository;
//
//    @Mock
//    private RecommendationRepository recommendationRepository;
//
//    @InjectMocks
//    private RecommendationLogService recommendationLogService;
//
//    private final Resource dummyResource = Resource.builder()
//            .awsResourceId("i-dummy-resource")
//            .serviceType(AwsServiceType.EC2)
//            .region("ap-northeast-2")
//            .isIdle(false)
//            .usageRate(50.0F)
//            .costUsd(10.0F)
//            .lastCheckedAt(LocalDateTime.now())
//            .build();
//
//    private final Recommendation dummyRecommendation = Recommendation.builder()
//            .resource(dummyResource)
//            .recommendationText("Test Rec")
//            .expectedSaving(5.0F)
//            .status("pending")
//            .build();
//
//    @DisplayName("RecommendationLog를 생성한다.")
//    @Test
//    void createRecommendationLog() {
//        // Given
//        RecommendationLogDto.CreateRequest createRequest = RecommendationLogDto.CreateRequest.builder()
//                .recommendationId(1L)
//                .userId(1L)
//                .action("accept")
//                .reason("비용 절감 필요")
//                .build();
//
//        RecommendationLog mockLog = RecommendationLog.builder()
//                .recommendation(dummyRecommendation)
//                .userId(1L)
//                .action("accept")
//                .reason("비용 절감 필요")
//                .build();
//
//        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(dummyRecommendation));
//        when(recommendationLogRepository.save(any(RecommendationLog.class))).thenReturn(mockLog);
//
//        // When
//        RecommendationLogDto.Response response = recommendationLogService.createRecommendationLog(createRequest);
//
//        // Then
//        assertThat(response.getUserId()).isEqualTo(1L);
//        assertThat(response.getAction()).isEqualTo("accept");
//        verify(recommendationLogRepository, times(1)).save(any(RecommendationLog.class));
//    }
//
//    @DisplayName("존재하지 않는 Recommendation ID로 RecommendationLog 생성 시 예외가 발생한다.")
//    @Test
//    void createRecommendationLogWithNonExistentRecommendation() {
//        // Given
//        RecommendationLogDto.CreateRequest createRequest = RecommendationLogDto.CreateRequest.builder()
//                .recommendationId(999L)
//                .userId(1L)
//                .action("accept")
//                .reason("비용 절감 필요")
//                .build();
//
//        when(recommendationRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThrows(ApplicationException.class, () -> recommendationLogService.createRecommendationLog(createRequest));
//        verify(recommendationLogRepository, never()).save(any(RecommendationLog.class));
//    }
//
//    @DisplayName("모든 RecommendationLog를 조회한다.")
//    @Test
//    void getAllRecommendationLogs() {
//        // Given
//        RecommendationLog log1 = RecommendationLog.builder().recommendation(dummyRecommendation).userId(1L).action("accept").reason("reason1").build();
//        RecommendationLog log2 = RecommendationLog.builder().recommendation(dummyRecommendation).userId(2L).action("ignore").reason("reason2").build();
//        when(recommendationLogRepository.findAll()).thenReturn(Arrays.asList(log1, log2));
//
//        // When
//        List<RecommendationLogDto.Response> responses = recommendationLogService.getAllRecommendationLogs();
//
//        // Then
//        assertThat(responses).hasSize(2);
//        assertThat(responses.get(0).getAction()).isEqualTo("accept");
//        assertThat(responses.get(1).getAction()).isEqualTo("ignore");
//    }
//
//    @DisplayName("ID로 RecommendationLog를 조회한다.")
//    @Test
//    void getRecommendationLogById() {
//        // Given
//        RecommendationLog log = RecommendationLog.builder().recommendation(dummyRecommendation).userId(1L).action("test").reason("reason").build();
//        when(recommendationLogRepository.findById(1L)).thenReturn(Optional.of(log));
//
//        // When
//        RecommendationLogDto.Response response = recommendationLogService.getRecommendationLogById(1L);
//
//        // Then
//        assertThat(response.getAction()).isEqualTo("test");
//    }
//
//    @DisplayName("존재하지 않는 ID로 RecommendationLog 조회 시 예외가 발생한다.")
//    @Test
//    void getRecommendationLogByIdNotFound() {
//        // Given
//        when(recommendationLogRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThrows(ApplicationException.class, () -> recommendationLogService.getRecommendationLogById(1L));
//    }
//
//    @DisplayName("Recommendation ID로 RecommendationLog 목록을 조회한다.")
//    @Test
//    void getRecommendationLogsByRecommendationId() {
//        // Given
//        RecommendationLog log1 = RecommendationLog.builder().recommendation(dummyRecommendation).userId(1L).action("act1").reason("res1").build();
//        RecommendationLog log2 = RecommendationLog.builder().recommendation(dummyRecommendation).userId(1L).action("act2").reason("res2").build();
//
//        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(dummyRecommendation));
//        when(recommendationLogRepository.findByRecommendation(dummyRecommendation)).thenReturn(Arrays.asList(log1, log2));
//
//        // When
//        List<RecommendationLogDto.Response> responses = recommendationLogService.getRecommendationLogsByRecommendationId(1L);
//
//        // Then
//        assertThat(responses).hasSize(2);
//        assertThat(responses.get(0).getAction()).isEqualTo("act1");
//        assertThat(responses.get(1).getAction()).isEqualTo("act2");
//    }
//
//    @DisplayName("RecommendationLog를 삭제한다.")
//    @Test
//    void deleteRecommendationLog() {
//        // Given
//        when(recommendationLogRepository.existsById(1L)).thenReturn(true);
//        doNothing().when(recommendationLogRepository).deleteById(1L);
//
//        // When
//        recommendationLogService.deleteRecommendationLog(1L);
//
//        // Then
//        verify(recommendationLogRepository, times(1)).deleteById(1L);
//    }
//
//    @DisplayName("존재하지 않는 ID의 RecommendationLog 삭제 시 예외가 발생한다.")
//    @Test
//    void deleteRecommendationLogNotFound() {
//        // Given
//        when(recommendationLogRepository.existsById(1L)).thenReturn(false);
//
//        // When & Then
//        assertThrows(ApplicationException.class, () -> recommendationLogService.deleteRecommendationLog(1L));
//        verify(recommendationLogRepository, never()).deleteById(anyLong());
//    }
//}