package com.team11.backend.domain.aws.repository;

import com.team11.backend.domain.aws.entity.AwsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AwsAccountRepository extends JpaRepository<AwsAccount, Long> {
    
    List<AwsAccount> findByUserUidAndIsActiveTrue(String userUid);
    
    List<AwsAccount> findByUserUid(String userUid);
    
    Optional<AwsAccount> findByIdAndUserUid(Long id, String userUid);
    
    Optional<AwsAccount> findByAwsAccountId(String awsAccountId);
    
    boolean existsByAwsAccountIdAndUserUid(String awsAccountId, String userUid);
}
