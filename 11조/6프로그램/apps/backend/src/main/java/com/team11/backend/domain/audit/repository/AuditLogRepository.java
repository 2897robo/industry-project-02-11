package com.team11.backend.domain.audit.repository;

import com.team11.backend.domain.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
