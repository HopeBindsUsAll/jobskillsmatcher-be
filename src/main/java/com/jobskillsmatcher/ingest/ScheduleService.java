package com.jobskillsmatcher.ingest;

import com.jobskillsmatcher.ingest.impl.jpa.IngestSchedule;
import com.jobskillsmatcher.ingest.port.rest.UpsertScheduleRequest;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {

    List<IngestSchedule> listAll();

    IngestSchedule create(UpsertScheduleRequest req);

    IngestSchedule update(UUID id, UpsertScheduleRequest req);

    void delete(UUID id);

    IngestSchedule get(UUID id);
}
