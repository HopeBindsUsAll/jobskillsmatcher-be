package com.jobskillsmatcher.auditlog.impl.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class AuditLogQueueAppender extends AppenderBase<ILoggingEvent> {

    public record QueuedEvent(long timestamp, String level, String loggerName, String message) {
    }

    public static final ThreadLocal<Boolean> SUPPRESS = ThreadLocal.withInitial(() -> false);

    private static final int CAPACITY = 4096;
    private static final BlockingQueue<QueuedEvent> QUEUE = new ArrayBlockingQueue<>(CAPACITY);

    @Override
    protected void append(ILoggingEvent event) {
        if (Boolean.TRUE.equals(SUPPRESS.get())) {
            return;
        }
        // offer() drops on overflow — never block the logging thread.
        QUEUE.offer(new QueuedEvent(
                event.getTimeStamp(),
                event.getLevel().levelStr,
                event.getLoggerName(),
                event.getFormattedMessage()));
    }


    public static List<QueuedEvent> drain(int max) {
        List<QueuedEvent> batch = new ArrayList<>(Math.min(max, QUEUE.size()));
        QUEUE.drainTo(batch, max);
        return batch;
    }
}
