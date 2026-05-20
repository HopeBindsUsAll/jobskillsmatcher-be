package com.jobskillsmatcher.auditlog;

import com.jobskillsmatcher.auditlog.impl.jpa.AuditLog;
import com.jobskillsmatcher.auditlog.model.LogCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;


public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    @Modifying
    @Query("delete from AuditLog a where a.category = :category and a.createdAt < :cutoff")
    int deleteByCategoryOlderThan(@Param("category") LogCategory category,
                                  @Param("cutoff") OffsetDateTime cutoff);
}
