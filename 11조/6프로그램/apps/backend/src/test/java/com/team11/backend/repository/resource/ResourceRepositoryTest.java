//package com.team11.backend.repository.resource;
//
//import com.team11.backend.domain.resource.entity.Resource;
//import com.team11.backend.domain.resource.entity.type.AwsServiceType;
//import com.team11.backend.domain.resource.repository.ResourceRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest
//class ResourceRepositoryTest {
//
//    @Autowired
//    private ResourceRepository resourceRepository;
//
//    @DisplayName("Resource를 저장하고 ID로 조회할 수 있다.")
//    @Test
//    void saveAndFindById() {
//        // Given
//        Resource resource = Resource.builder()
//                .userId(1L)
//                .awsResourceId("i-0a1b2c3d4e5f6a7b8")
//                .serviceType(AwsServiceType.EC2)
//                .region("ap-northeast-2")
//                .isIdle(true)
//                .usageRate(10.5F)
//                .costUsd(0.5F)
//                .lastCheckedAt(LocalDateTime.now())
//                .build();
//
//        // When
//        Resource savedResource = resourceRepository.save(resource);
//        Optional<Resource> foundResource = resourceRepository.findById(savedResource.getId());
//
//        // Then
//        assertThat(foundResource).isPresent();
//        assertThat(foundResource.get().getId()).isEqualTo(savedResource.getId());
//        assertThat(foundResource.get().getAwsResourceId()).isEqualTo("i-0a1b2c3d4e5f6a7b8");
//    }
//
//    @DisplayName("모든 Resource를 조회할 수 있다.")
//    @Test
//    void findAllResources() {
//        // Given
//        Resource resource1 = Resource.builder()
//                .userId(1L)
//                .awsResourceId("i-00000000000000001")
//                .serviceType(AwsServiceType.EC2)
//                .region("ap-northeast-2")
//                .isIdle(false)
//                .usageRate(70.0F)
//                .costUsd(20.0F)
//                .lastCheckedAt(LocalDateTime.now())
//                .build();
//        Resource resource2 = Resource.builder()
//                .userId(1L)
//                .awsResourceId("i-00000000000000002")
//                .serviceType(AwsServiceType.RDS)
//                .region("us-east-1")
//                .isIdle(true)
//                .usageRate(5.0F)
//                .costUsd(0.1F)
//                .lastCheckedAt(LocalDateTime.now())
//                .build();
//        resourceRepository.save(resource1);
//        resourceRepository.save(resource2);
//
//        // When
//        List<Resource> resources = resourceRepository.findAll();
//
//        // Then
//        assertThat(resources).hasSize(2);
//        assertThat(resources).containsExactlyInAnyOrder(resource1, resource2);
//    }
//
//    @DisplayName("UserId로 Resource 목록을 조회할 수 있다.")
//    @Test
//    void findByUserId() {
//        // Given
//        Resource resource1 = Resource.builder()
//                .userId(1L)
//                .awsResourceId("i-user1-1")
//                .serviceType(AwsServiceType.EC2)
//                .region("ap-northeast-2")
//                .isIdle(false)
//                .usageRate(70.0F)
//                .costUsd(20.0F)
//                .lastCheckedAt(LocalDateTime.now())
//                .build();
//        Resource resource2 = Resource.builder()
//                .userId(1L)
//                .awsResourceId("i-user1-2")
//                .serviceType(AwsServiceType.RDS)
//                .region("us-east-1")
//                .isIdle(true)
//                .usageRate(5.0F)
//                .costUsd(0.1F)
//                .lastCheckedAt(LocalDateTime.now())
//                .build();
//        Resource resource3 = Resource.builder()
//                .userId(2L)
//                .awsResourceId("i-user2-1")
//                .serviceType(AwsServiceType.S3)
//                .region("eu-west-1")
//                .isIdle(false)
//                .usageRate(90.0F)
//                .costUsd(1.0F)
//                .lastCheckedAt(LocalDateTime.now())
//                .build();
//        resourceRepository.save(resource1);
//        resourceRepository.save(resource2);
//        resourceRepository.save(resource3);
//
//        // When
//        List<Resource> user1Resources = resourceRepository.findByUserId(1L);
//        List<Resource> user2Resources = resourceRepository.findByUserId(2L);
//        List<Resource> user3Resources = resourceRepository.findByUserId(3L); // 없는 사용자 ID
//
//        // Then
//        assertThat(user1Resources).hasSize(2);
//        assertThat(user1Resources).containsExactlyInAnyOrder(resource1, resource2);
//        assertThat(user2Resources).hasSize(1);
//        assertThat(user2Resources).containsExactlyInAnyOrder(resource3);
//        assertThat(user3Resources).isEmpty();
//    }
//
//    @DisplayName("AWS Resource ID로 Resource를 조회할 수 있다.")
//    @Test
//    void findByAwsResourceId() {
//        // Given
//        Resource resource = Resource.builder()
//                .userId(1L)
//                .awsResourceId("i-unique-aws-resource-id")
//                .serviceType(AwsServiceType.EC2)
//                .region("ap-northeast-2")
//                .isIdle(false)
//                .usageRate(70.0F)
//                .costUsd(20.0F)
//                .lastCheckedAt(LocalDateTime.now())
//                .build();
//        resourceRepository.save(resource);
//
//        // When
//        Optional<Resource> foundResource = resourceRepository.findByAwsResourceId("i-unique-aws-resource-id");
//        Optional<Resource> notFoundResource = resourceRepository.findByAwsResourceId("i-nonexistent-id");
//
//        // Then
//        assertThat(foundResource).isPresent();
//        assertThat(foundResource.get().getAwsResourceId()).isEqualTo("i-unique-aws-resource-id");
//        assertThat(notFoundResource).isNotPresent();
//    }
//
//
//    @DisplayName("Resource를 업데이트할 수 있다.")
//    @Test
//    void updateResource() {
//        // Given
//        Resource resource = Resource.builder()
//                .userId(1L)
//                .awsResourceId("i-to-be-updated")
//                .serviceType(AwsServiceType.EC2)
//                .region("ap-northeast-2")
//                .isIdle(true)
//                .usageRate(10.0F)
//                .costUsd(0.5F)
//                .lastCheckedAt(LocalDateTime.now())
//                .build();
//        Resource savedResource = resourceRepository.save(resource);
//
//        // When
//        savedResource.update(AwsServiceType.RDS, "us-east-1", false, 80.0F, 15.0F, LocalDateTime.now().plusDays(1));
//        Resource updatedResource = resourceRepository.save(savedResource); // save를 통해 변경 감지 및 DB 반영
//
//        // Then
//        assertThat(updatedResource.getServiceType()).isEqualTo(AwsServiceType.RDS);
//        assertThat(updatedResource.getRegion()).isEqualTo("us-east-1");
//        assertThat(updatedResource.getIsIdle()).isFalse();
//        assertThat(updatedResource.getUsageRate()).isEqualTo(80.0F);
//        assertThat(updatedResource.getCostUsd()).isEqualTo(15.0F);
//    }
//
//    @DisplayName("Resource를 삭제할 수 있다.")
//    @Test
//    void deleteResource() {
//        // Given
//        Resource resource = Resource.builder()
//                .userId(1L)
//                .awsResourceId("i-to-be-deleted")
//                .serviceType(AwsServiceType.EC2)
//                .region("ap-northeast-2")
//                .isIdle(false)
//                .usageRate(50.0F)
//                .costUsd(1.0F)
//                .lastCheckedAt(LocalDateTime.now())
//                .build();
//        Resource savedResource = resourceRepository.save(resource);
//
//        // When
//        resourceRepository.deleteById(savedResource.getId());
//        Optional<Resource> foundResource = resourceRepository.findById(savedResource.getId());
//
//        // Then
//        assertThat(foundResource).isNotPresent();
//    }
//}