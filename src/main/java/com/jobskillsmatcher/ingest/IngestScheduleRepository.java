package com.jobskillsmatcher.ingest;

import com.jobskillsmatcher.ingest.impl.jpa.IngestSchedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IngestScheduleRepository extends JpaRepository<IngestSchedule, UUID> {

    List<IngestSchedule> findAllByEnabledTrue();
}
