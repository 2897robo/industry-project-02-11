package com.team11.backend.domain.aws.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "aws_accounts", indexes = {
    @Index(name = "idx_aws_accounts_user_uid", columnList = "user_uid")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AwsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uid", nullable = false)
    private String userUid;

    @Column(name = "account_alias")
    private String accountAlias;

    @Column(name = "aws_account_id", unique = true)
    private String awsAccountId;

    @Column(name = "access_key_id", nullable = false)
    private String accessKeyId;

    @Column(name = "secret_access_key", nullable = false)
    private String secretAccessKey; // 암호화 필요

    @Column(name = "region", length = 50)
    @Builder.Default
    private String region = "ap-northeast-2";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 비즈니스 메서드
    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void updateCredentials(String accessKeyId, String secretAccessKey) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }

    public void updateAccountInfo(String accountAlias, String region) {
        if (accountAlias != null) {
            this.accountAlias = accountAlias;
        }
        if (region != null) {
            this.region = region;
        }
    }
}
