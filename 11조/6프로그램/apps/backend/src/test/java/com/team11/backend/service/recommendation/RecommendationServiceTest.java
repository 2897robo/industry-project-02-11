//package com.team11.backend.service.recommendation;
//
//import com.team11.backend.domain.recommendation.entity.Recommendation;
//import com.team11.backend.domain.recommendation.dto.RecommendationDto;
//import com.team11.backend.domain.recommendation.repository.RecommendationRepository;
//import com.team11.backend.domain.recommendation.service.RecommendationService;
//import com.team11.backend.domain.resource.entity.Resource;
//import com.team11.backend.domain.resource.entity.type.AwsServiceType;
//import com.team11.backend.domain.resource.repository.ResourceRepository;
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
//@ExtendWith(MockitoExtension.class) // Mockito를 JUnit 5에서 사용하기 위함
//class RecommendationServiceTest {
//
//    @Mock // 가짜 객체
//    private RecommendationRepository recommendationRepository;
//
//    @Mock // 가짜 객체
//    private ResourceRepository resourceRepository;
//
//    @InjectMocks // Mock 객체들을 주입받을 대상 (테스트할 서비스)
//    private RecommendationService recommendationService;
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
//    @DisplayName("Recommendation을 생성한다.")
//    @Test
//    void createRecommendation() {
//        // Given
//        RecommendationDto.CreateRequest createRequest = RecommendationDto.CreateRequest.builder()
//                .resourceId(1L)
//                .recommendationText("EC2 t3.small로 다운사이징")
//                .expectedSaving(5.0F)
//                .status("pending")
//                .build();
//
//        Recommendation mockRecommendation = Recommendation.builder()
//                .resource(dummyResource)
//                .recommendationText("EC2 t3.small로 다운사이징")
//                .expectedSaving(5.0F)
//                .status("pending")
//                .build();
//        // ID는 save 시점에 부여되므로, 목 객체 생성 시에는 ID를 설정하지 않거나,
//        // 반환되는 객체에 ID를 설정한 후 사용해야 합니다.
//        // 여기서는 save() 메소드가 실제 DB처럼 ID를 할당한다고 가정합니다.
//        when(resourceRepository.findById(1L)).thenReturn(Optional.of(dummyResource));
//        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(mockRecommendation);
//
//        // When
//        RecommendationDto.Response response = recommendationService.createRecommendation(createRequest);
//
//        // Then
//        assertThat(response.getRecommendationText()).isEqualTo("EC2 t3.small로 다운사이징");
//        verify(recommendationRepository, times(1)).save(any(Recommendation.class));
//    }
//
//    @DisplayName("존재하지 않는 Resource ID로 Recommendation 생성 시 예외가 발생한다.")
//    @Test
//    void createRecommendationWithNonExistentResource() {
//        // Given
//        RecommendationDto.CreateRequest createRequest = RecommendationDto.CreateRequest.builder()
//                .resourceId(999L) // 존재하지 않는 ID
//                .recommendationText("EC2 t3.small로 다운사이징")
//                .expectedSaving(5.0F)
//                .status("pending")
//                .build();
//
//        when(resourceRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThrows(ApplicationException.class, () -> recommendationService.createRecommendation(createRequest));
//        verify(recommendationRepository, never()).save(any(Recommendation.class)); // save 메소드 호출되지 않음
//    }
//
//
//    @DisplayName("모든 Recommendation을 조회한다.")
//    @Test
//    void getAllRecommendations() {
//        // Given
//        Recommendation rec1 = Recommendation.builder().resource(dummyResource).recommendationText("Rec1").expectedSaving(1.0F).status("pending").build();
//        Recommendation rec2 = Recommendation.builder().resource(dummyResource).recommendationText("Rec2").expectedSaving(2.0F).status("accepted").build();
//        when(recommendationRepository.findAll()).thenReturn(Arrays.asList(rec1, rec2));
//
//        // When
//        List<RecommendationDto.Response> responses = recommendationService.getAllRecommendations();
//
//        // Then
//        assertThat(responses).hasSize(2);
//        assertThat(responses.get(0).getRecommendationText()).isEqualTo("Rec1");
//        assertThat(responses.get(1).getRecommendationText()).isEqualTo("Rec2");
//    }
//
//    @DisplayName("ID로 Recommendation을 조회한다.")
//    @Test
//    void getRecommendationById() {
//        // Given
//        Recommendation recommendation = Recommendation.builder().resource(dummyResource).recommendationText("Test Rec").expectedSaving(5.0F).status("pending").build();
//        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));
//
//        // When
//        RecommendationDto.Response response = recommendationService.getRecommendationById(1L);
//
//        // Then
//        assertThat(response.getRecommendationText()).isEqualTo("Test Rec");
//    }
//
//    @DisplayName("존재하지 않는 ID로 Recommendation 조회 시 예외가 발생한다.")
//    @Test
//    void getRecommendationByIdNotFound() {
//        // Given
//        when(recommendationRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThrows(ApplicationException.class, () -> recommendationService.getRecommendationById(1L));
//    }
//
//    @DisplayName("Resource ID로 Recommendation 목록을 조회한다.")
//    @Test
//    void getRecommendationsByResourceId() {
//        // Given
//        Recommendation rec1 = Recommendation.builder().resource(dummyResource).recommendationText("Rec1 by Res").expectedSaving(1.0F).status("pending").build();
//        Recommendation rec2 = Recommendation.builder().resource(dummyResource).recommendationText("Rec2 by Res").expectedSaving(2.0F).status("accepted").build();
//
//        when(resourceRepository.findById(1L)).thenReturn(Optional.of(dummyResource));
//        when(recommendationRepository.findByResource(dummyResource)).thenReturn(Arrays.asList(rec1, rec2));
//
//        // When
//        List<RecommendationDto.Response> responses = recommendationService.getRecommendationsByResourceId(1L);
//
//        // Then
//        assertThat(responses).hasSize(2);
//        assertThat(responses.get(0).getRecommendationText()).isEqualTo("Rec1 by Res");
//        assertThat(responses.get(1).getRecommendationText()).isEqualTo("Rec2 by Res");
//    }
//
//    @DisplayName("Recommendation을 업데이트한다.")
//    @Test
//    void updateRecommendation() {
//        // Given
//        Recommendation existingRecommendation = Recommendation.builder().resource(dummyResource).recommendationText("Old Text").expectedSaving(5.0F).status("pending").build();
//        RecommendationDto.UpdateRequest updateRequest = RecommendationDto.UpdateRequest.builder()
//                .recommendationText("New Text")
//                .expectedSaving(10.0F)
//                .status("completed")
//                .build();
//
//        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(existingRecommendation));
//
//        // When
//        RecommendationDto.Response response = recommendationService.updateRecommendation(1L, updateRequest);
//
//        // Then
//        assertThat(response.getRecommendationText()).isEqualTo("New Text");
//        assertThat(response.getExpectedSaving()).isEqualTo(10.0F);
//        assertThat(response.getStatus()).isEqualTo("completed");
//        // save()는 @Transactional에 의해 자동 호출되므로 명시적으로 verify하지 않는다.
//        // 하지만 update() 메소드가 호출되었는지 확인하려면 verify(existingRecommendation).update(...) 와 같이 할 수 있다.
//    }
//
//    @DisplayName("존재하지 않는 ID의 Recommendation 업데이트 시 예외가 발생한다.")
//    @Test
//    void updateRecommendationNotFound() {
//        // Given
//        RecommendationDto.UpdateRequest updateRequest = RecommendationDto.UpdateRequest.builder()
//                .recommendationText("New Text")
//                .expectedSaving(10.0F)
//                .status("completed")
//                .build();
//        when(recommendationRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThrows(ApplicationException.class, () -> recommendationService.updateRecommendation(1L, updateRequest));
//    }
//
//    @DisplayName("Recommendation을 삭제한다.")
//    @Test
//    void deleteRecommendation() {
//        // Given
//        when(recommendationRepository.existsById(1L)).thenReturn(true);
//        doNothing().when(recommendationRepository).deleteById(1L);
//
//        // When
//        recommendationService.deleteRecommendation(1L);
//
//        // Then
//        verify(recommendationRepository, times(1)).deleteById(1L);
//    }
//
//    @DisplayName("존재하지 않는 ID의 Recommendation 삭제 시 예외가 발생한다.")
//    @Test
//    void deleteRecommendationNotFound() {
//        // Given
//        when(recommendationRepository.existsById(1L)).thenReturn(false);
//
//        // When & Then
//        assertThrows(ApplicationException.class, () -> recommendationService.deleteRecommendation(1L));
//        verify(recommendationRepository, never()).deleteById(anyLong()); // deleteById 호출되지 않음
//    }
//}
