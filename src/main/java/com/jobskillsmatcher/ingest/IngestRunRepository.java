package com.jobskillsmatcher.ingest;

import com.jobskillsmatcher.ingest.impl.jpa.IngestRun;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IngestRunRepository extends JpaRepository<IngestRun, UUID> {

    Page<IngestRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
