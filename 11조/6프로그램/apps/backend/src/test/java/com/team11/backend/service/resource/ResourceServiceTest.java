//package com.team11.backend.service.resource;
//
//import com.team11.backend.domain.resource.entity.Resource;
//import com.team11.backend.domain.resource.entity.type.AwsServiceType;
//import com.team11.backend.domain.resource.dto.ResourceDto;
//import com.team11.backend.domain.resource.repository.ResourceRepository;
//import com.team11.backend.commons.exception.ApplicationException;
//import com.team11.backend.domain.resource.service.ResourceService;
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
//class ResourceServiceTest {
//
//    @Mock
//    private ResourceRepository resourceRepository;
//
//    @InjectMocks
//    private ResourceService resourceService;
//
//    @DisplayName("Resource를 생성한다.")
//    @Test
//    void createResource() {
//        // Given
//        ResourceDto.CreateRequest createRequest = ResourceDto.CreateRequest.builder()
//                .userId(1L)
//                .awsResourceId("i-test-create")
//                .serviceType(AwsServiceType.EC2)
//                .region("ap-northeast-2")
//                .isIdle(false)
//                .usageRate(70.0F)
//                .costUsd(15.0F)
//                .lastCheckedAt(LocalDateTime.now())
//                .build();
//
//        Resource mockResource = Resource.builder()
//                .userId(1L)
//                .awsResourceId("i-test-create")
//                .serviceType(AwsServiceType.EC2)
//                .region("ap-northeast-2")
//                .isIdle(false)
//                .usageRate(70.0F)
//                .costUsd(15.0F)
//                .lastCheckedAt(LocalDateTime.now())
//                .build();
//
//        when(resourceRepository.save(any(Resource.class))).thenReturn(mockResource);
//
//        // When
//        ResourceDto.Response response = resourceService.createResource(createRequest);
//
//        // Then
//        assertThat(response.getAwsResourceId()).isEqualTo("i-test-create");
//        assertThat(response.getServiceType()).isEqualTo(AwsServiceType.EC2);
//        verify(resourceRepository, times(1)).save(any(Resource.class));
//    }
//
//    @DisplayName("모든 Resource를 조회한다.")
//    @Test
//    void getAllResources() {
//        // Given
//        Resource res1 = Resource.builder().userId(1L).awsResourceId("res1").serviceType(AwsServiceType.EC2).region("r1").isIdle(false).usageRate(50F).costUsd(10F).lastCheckedAt(LocalDateTime.now()).build();
//        Resource res2 = Resource.builder().userId(1L).awsResourceId("res2").serviceType(AwsServiceType.RDS).region("r2").isIdle(true).usageRate(10F).costUsd(2F).lastCheckedAt(LocalDateTime.now()).build();
//        when(resourceRepository.findAll()).thenReturn(Arrays.asList(res1, res2));
//
//        // When
//        List<ResourceDto.Response> responses = resourceService.getAllResources();
//
//        // Then
//        assertThat(responses).hasSize(2);
//        assertThat(responses.get(0).getAwsResourceId()).isEqualTo("res1");
//        assertThat(responses.get(1).getAwsResourceId()).isEqualTo("res2");
//    }
//
//    @DisplayName("ID로 Resource를 조회한다.")
//    @Test
//    void getResourceById() {
//        // Given
//        Resource resource = Resource.builder().userId(1L).awsResourceId("test-res").serviceType(AwsServiceType.EC2).region("ap-northeast-2").isIdle(false).usageRate(50F).costUsd(10F).lastCheckedAt(LocalDateTime.now()).build();
//        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
//
//        // When
//        ResourceDto.Response response = resourceService.getResourceById(1L);
//
//        // Then
//        assertThat(response.getAwsResourceId()).isEqualTo("test-res");
//    }
//
//    @DisplayName("존재하지 않는 ID로 Resource 조회 시 예외가 발생한다.")
//    @Test
//    void getResourceByIdNotFound() {
//        // Given
//        when(resourceRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThrows(ApplicationException.class, () -> resourceService.getResourceById(1L));
//    }
//
//    @DisplayName("User ID로 Resource 목록을 조회한다.")
//    @Test
//    void getResourcesByUserId() {
//        // Given
//        Resource res1 = Resource.builder().userId(1L).awsResourceId("user1-res1").serviceType(AwsServiceType.EC2).region("r1").isIdle(false).usageRate(50F).costUsd(10F).lastCheckedAt(LocalDateTime.now()).build();
//        Resource res2 = Resource.builder().userId(1L).awsResourceId("user1-res2").serviceType(AwsServiceType.RDS).region("r2").isIdle(true).usageRate(10F).costUsd(2F).lastCheckedAt(LocalDateTime.now()).build();
//        when(resourceRepository.findByUserId(1L)).thenReturn(Arrays.asList(res1, res2));
//
//        // When
//        List<ResourceDto.Response> responses = resourceService.getResourcesByUserId(1L);
//
//        // Then
//        assertThat(responses).hasSize(2);
//        assertThat(responses.get(0).getAwsResourceId()).isEqualTo("user1-res1");
//        assertThat(responses.get(1).getAwsResourceId()).isEqualTo("user1-res2");
//    }
//
//    @DisplayName("AWS Resource ID로 Resource를 조회한다.")
//    @Test
//    void getResourceByAwsResourceId() {
//        // Given
//        Resource resource = Resource.builder().userId(1L).awsResourceId("aws-res-id").serviceType(AwsServiceType.EC2).region("ap-northeast-2").isIdle(false).usageRate(50F).costUsd(10F).lastCheckedAt(LocalDateTime.now()).build();
//        when(resourceRepository.findByAwsResourceId("aws-res-id")).thenReturn(Optional.of(resource));
//
//        // When
//        ResourceDto.Response response = resourceService.getResourceByAwsResourceId("aws-res-id");
//
//        // Then
//        assertThat(response.getAwsResourceId()).isEqualTo("aws-res-id");
//    }
//
//    @DisplayName("존재하지 않는 AWS Resource ID로 Resource 조회 시 예외가 발생한다.")
//    @Test
//    void getResourceByAwsResourceIdNotFound() {
//        // Given
//        when(resourceRepository.findByAwsResourceId("non-existent")).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThrows(ApplicationException.class, () -> resourceService.getResourceByAwsResourceId("non-existent"));
//    }
//
//    @DisplayName("Resource를 업데이트한다.")
//    @Test
//    void updateResource() {
//        // Given
//        Resource existingResource = Resource.builder().userId(1L).awsResourceId("old-res").serviceType(AwsServiceType.EC2).region("old-region").isIdle(true).usageRate(10F).costUsd(1F).lastCheckedAt(LocalDateTime.now()).build();
//        ResourceDto.UpdateRequest updateRequest = ResourceDto.UpdateRequest.builder()
//                .serviceType(AwsServiceType.RDS)
//                .region("new-region")
//                .isIdle(false)
//                .usageRate(80.0F)
//                .costUsd(20.0F)
//                .lastCheckedAt(LocalDateTime.now().plusHours(1))
//                .build();
//
//        when(resourceRepository.findById(1L)).thenReturn(Optional.of(existingResource));
//
//        // When
//        ResourceDto.Response response = resourceService.updateResource(1L, updateRequest);
//
//        // Then
//        assertThat(response.getServiceType()).isEqualTo(AwsServiceType.RDS);
//        assertThat(response.getRegion()).isEqualTo("new-region");
//        assertThat(response.getIsIdle()).isFalse();
//        assertThat(response.getUsageRate()).isEqualTo(80.0F);
//        assertThat(response.getCostUsd()).isEqualTo(20.0F);
//    }
//
//    @DisplayName("존재하지 않는 ID의 Resource 업데이트 시 예외가 발생한다.")
//    @Test
//    void updateResourceNotFound() {
//        // Given
//        ResourceDto.UpdateRequest updateRequest = ResourceDto.UpdateRequest.builder()
//                .serviceType(AwsServiceType.RDS)
//                .region("new-region")
//                .isIdle(false)
//                .usageRate(80.0F)
//                .costUsd(20.0F)
//                .lastCheckedAt(LocalDateTime.now().plusHours(1))
//                .build();
//        when(resourceRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThrows(ApplicationException.class, () -> resourceService.updateResource(1L, updateRequest));
//    }
//
//    @DisplayName("Resource를 삭제한다.")
//    @Test
//    void deleteResource() {
//        // Given
//        when(resourceRepository.existsById(1L)).thenReturn(true);
//        doNothing().when(resourceRepository).deleteById(1L);
//
//        // When
////        resourceService.deleteResource(1L);
//
//        // Then
//        verify(resourceRepository, times(1)).deleteById(1L);
//    }
//
//    @DisplayName("존재하지 않는 ID의 Resource 삭제 시 예외가 발생한다.")
//    @Test
//    void deleteResourceNotFound() {
//        // Given
//        when(resourceRepository.existsById(1L)).thenReturn(false);
//
//        // When & Then
//        assertThrows(ApplicationException.class, () -> resourceService.deleteResource(1L));
//        verify(resourceRepository, never()).deleteById(anyLong());
//    }
//}
