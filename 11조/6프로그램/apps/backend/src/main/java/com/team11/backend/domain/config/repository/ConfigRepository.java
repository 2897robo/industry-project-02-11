package com.team11.backend.domain.config.repository;

import com.team11.backend.domain.config.entity.Config;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigRepository extends JpaRepository<Config, Long> {
    Optional<Config> findByUserUid(String userUid);
    void deleteByUserUid(String userUid);
}
