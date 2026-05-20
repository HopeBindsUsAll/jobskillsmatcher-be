package com.jobskillsmatcher.matching;

import com.jobskillsmatcher.matching.impl.jpa.ReadinessSnapshot;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReadinessSnapshotRepository extends JpaRepository<ReadinessSnapshot, UUID> {

    List<ReadinessSnapshot> findAllByStudentIdOrderByCapturedAtDesc(UUID studentId, Pageable pageable);
}
