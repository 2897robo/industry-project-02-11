package com.team11.backend.domain.config.service;

import com.team11.backend.commons.exception.ApplicationException;
import com.team11.backend.commons.exception.payload.ErrorStatus;
import com.team11.backend.domain.config.dto.request.CreateConfigRequest;
import com.team11.backend.domain.config.dto.request.UpdateConfigRequest;
import com.team11.backend.domain.config.dto.response.ReadConfigResponse;
import com.team11.backend.domain.config.entity.Config;
import com.team11.backend.domain.config.repository.ConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfigService {

    private final ConfigRepository configRepository;

    public long createConfig(String userUid, CreateConfigRequest request) {
        return configRepository.save(request.toEntity(userUid)).getId();
    }

    public void updateConfig(UpdateConfigRequest request) {
        Config config = configRepository.findById(request.id())
                .orElseThrow(() -> new ApplicationException(
                        ErrorStatus.toErrorStatus("해당하는 config가 없습니다.", 404, LocalDateTime.now())
                ));

        if(request.idleThreshold() != null && !request.idleThreshold().equals(config.getIdleThreshold())) {
            config.updateIdleThreshold(request.idleThreshold());
        }

        if(request.budgetLimit() != null && !request.budgetLimit().equals(config.getBudgetLimit())) {
            config.updateBudgetLimit(request.budgetLimit());
        }
    }

    @Transactional(readOnly = true)
    public ReadConfigResponse getByUserUid(String userUid) {

        Config config = configRepository.findByUserUid(userUid)
                .orElseThrow(() -> new ApplicationException(
                        ErrorStatus.toErrorStatus("해당하는 config가 없습니다.", 404, LocalDateTime.now())
                ));

        return ReadConfigResponse.fromEntity(config);
    }

    public void deleteByUserUid(String userUid) {
        configRepository.deleteByUserUid(userUid);
    }
}
