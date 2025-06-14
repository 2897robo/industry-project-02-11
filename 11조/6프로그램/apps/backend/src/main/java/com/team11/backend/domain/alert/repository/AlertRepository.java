package com.team11.backend.domain.alert.repository;

import com.team11.backend.domain.alert.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUserUid(String userUid);
}
