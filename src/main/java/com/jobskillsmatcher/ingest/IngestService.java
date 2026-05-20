package com.jobskillsmatcher.ingest;

import com.jobskillsmatcher.ingest.impl.client.JSearchJob;
import com.jobskillsmatcher.ingest.impl.jpa.IngestRun;
import com.jobskillsmatcher.ingest.impl.jpa.IngestSchedule;
import com.jobskillsmatcher.ingest.model.IngestRequest;

public interface IngestService {

    IngestRun runOnce(IngestRequest request);

    boolean storeJob(JSearchJob raw, IngestRequest request);

    IngestRun runForSchedule(IngestSchedule schedule);
}
