package com.team11.backend.domain.alert.service;

import com.team11.backend.domain.alert.dto.request.CreateAlertRequest;
import com.team11.backend.domain.alert.dto.response.ReadAlertResponse;
import com.team11.backend.domain.alert.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;

    public void createAlert(CreateAlertRequest request) {
        alertRepository.save(request.toEntity());
    }

    public List<ReadAlertResponse> getByUserUid(String uid) {
        return alertRepository.findByUserUid(uid).stream().map(ReadAlertResponse::fromEntity).toList();
    }
}
