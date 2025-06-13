package com.team11.backend.domain.aws.service;

import com.team11.backend.commons.exception.ApplicationException;
import com.team11.backend.commons.exception.payload.ErrorStatus;
import com.team11.backend.commons.util.AES256Cipher;
import com.team11.backend.domain.aws.dto.AwsAccountDto;
import com.team11.backend.domain.aws.entity.AwsAccount;
import com.team11.backend.domain.aws.repository.AwsAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AwsAccountService {

    private final AwsAccountRepository awsAccountRepository;
    private final AES256Cipher aes256Cipher;

    @Transactional
    public AwsAccountDto.Response createAwsAccount(String userUid, AwsAccountDto.CreateRequest request) {
        // 중복 확인
        if (awsAccountRepository.existsByAwsAccountIdAndUserUid(request.getAwsAccountId(), userUid)) {
            throw new ApplicationException(
                ErrorStatus.toErrorStatus("이미 등록된 AWS 계정입니다.", 409, LocalDateTime.now())
            );
        }

        try {
            // 비밀 키 암호화
            String encryptedSecretKey = aes256Cipher.encrypt(request.getSecretAccessKey());

            AwsAccount awsAccount = AwsAccount.builder()
                    .userUid(userUid)
                    .accountAlias(request.getAccountAlias())
                    .awsAccountId(request.getAwsAccountId())
                    .accessKeyId(request.getAccessKeyId())
                    .secretAccessKey(encryptedSecretKey)
                    .region(request.getRegion())
                    .build();

            AwsAccount saved = awsAccountRepository.save(awsAccount);
            log.info("AWS 계정 등록 완료: userId={}, awsAccountId={}", userUid, saved.getAwsAccountId());

            return AwsAccountDto.Response.from(saved);
        } catch (Exception e) {
            log.error("AWS 계정 등록 실패: {}", e.getMessage());
            throw new ApplicationException(
                ErrorStatus.toErrorStatus("AWS 계정 등록 중 오류가 발생했습니다.", 500, LocalDateTime.now())
            );
        }
    }

    public List<AwsAccountDto.Response> getMyAwsAccounts(String userUid) {
        return awsAccountRepository.findByUserUidAndIsActiveTrue(userUid).stream()
                .map(AwsAccountDto.Response::from)
                .collect(Collectors.toList());
    }

    public AwsAccountDto.Response getAwsAccount(String userUid, Long accountId) {
        AwsAccount account = awsAccountRepository.findByIdAndUserUid(accountId, userUid)
                .orElseThrow(() -> new ApplicationException(
                    ErrorStatus.toErrorStatus("AWS 계정을 찾을 수 없습니다.", 404, LocalDateTime.now())
                ));
        
        return AwsAccountDto.Response.from(account);
    }

    @Transactional
    public void updateAwsAccount(String userUid, Long accountId, AwsAccountDto.UpdateRequest request) {
        AwsAccount account = awsAccountRepository.findByIdAndUserUid(accountId, userUid)
                .orElseThrow(() -> new ApplicationException(
                    ErrorStatus.toErrorStatus("AWS 계정을 찾을 수 없습니다.", 404, LocalDateTime.now())
                ));

        account.updateAccountInfo(request.getAccountAlias(), request.getRegion());
        
        if (request.getAccessKeyId() != null && request.getSecretAccessKey() != null) {
            try {
                String encryptedSecretKey = aes256Cipher.encrypt(request.getSecretAccessKey());
                account.updateCredentials(request.getAccessKeyId(), encryptedSecretKey);
            } catch (Exception e) {
                log.error("AWS 자격 증명 업데이트 실패: {}", e.getMessage());
                throw new ApplicationException(
                    ErrorStatus.toErrorStatus("AWS 자격 증명 업데이트 중 오류가 발생했습니다.", 500, LocalDateTime.now())
                );
            }
        }
    }

    @Transactional
    public void deactivateAwsAccount(String userUid, Long accountId) {
        AwsAccount account = awsAccountRepository.findByIdAndUserUid(accountId, userUid)
                .orElseThrow(() -> new ApplicationException(
                    ErrorStatus.toErrorStatus("AWS 계정을 찾을 수 없습니다.", 404, LocalDateTime.now())
                ));
        
        account.deactivate();
        log.info("AWS 계정 비활성화: userId={}, accountId={}", userUid, accountId);
    }

    // AWS SDK 사용 시 복호화된 자격 증명을 반환하는 내부 메서드
    public AwsAccountDto.Credentials getDecryptedCredentials(String userUid, Long accountId) {
        AwsAccount account = awsAccountRepository.findByIdAndUserUid(accountId, userUid)
                .orElseThrow(() -> new ApplicationException(
                    ErrorStatus.toErrorStatus("AWS 계정을 찾을 수 없습니다.", 404, LocalDateTime.now())
                ));

        try {
            String decryptedSecretKey = aes256Cipher.decrypt(account.getSecretAccessKey());
            return AwsAccountDto.Credentials.builder()
                    .accessKeyId(account.getAccessKeyId())
                    .secretAccessKey(decryptedSecretKey)
                    .region(account.getRegion())
                    .build();
        } catch (Exception e) {
            log.error("AWS 자격 증명 복호화 실패: {}", e.getMessage());
            throw new ApplicationException(
                ErrorStatus.toErrorStatus("AWS 자격 증명 복호화 중 오류가 발생했습니다.", 500, LocalDateTime.now())
            );
        }
    }
}
