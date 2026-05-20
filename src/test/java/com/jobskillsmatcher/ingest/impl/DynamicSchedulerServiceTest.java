package com.jobskillsmatcher.ingest.impl;

import com.jobskillsmatcher.ingest.IngestService;

import com.jobskillsmatcher.ingest.impl.jpa.IngestSchedule;
import com.jobskillsmatcher.ingest.IngestScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicSchedulerServiceTest {

    @Mock private TaskScheduler taskScheduler;
    @Mock private IngestScheduleRepository scheduleRepository;
    @Mock private IngestService ingestService;

    private DynamicSchedulerService scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DynamicSchedulerService(taskScheduler, scheduleRepository, ingestService);
    }

    @Test
    void register_schedulesFutureWithCronTrigger() {
        IngestSchedule schedule = makeSchedule("0 0 6 * * *");
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));

        scheduler.register(schedule);

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(taskScheduler).schedule(any(Runnable.class), triggerCaptor.capture());
        assertThat(triggerCaptor.getValue()).isInstanceOf(CronTrigger.class);
        assertThat(scheduler.hasFuture(schedule.getId())).isTrue();
    }

    @Test
    void register_secondCallCancelsPreviousFutureAndReplacesIt() {
        IngestSchedule schedule = makeSchedule("0 0 6 * * *");
        ScheduledFuture<?> oldFuture = mock(ScheduledFuture.class);
        ScheduledFuture<?> newFuture = mock(ScheduledFuture.class);
        doReturn(oldFuture)
                .doReturn(newFuture)
                .when(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));

        scheduler.register(schedule);
        schedule.setCronExpression("0 0 12 * * *");
        scheduler.register(schedule);

        verify(oldFuture).cancel(false);
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Trigger.class));
        assertThat(scheduler.hasFuture(schedule.getId())).isTrue();
    }

    @Test
    void cancel_cancelsActiveFutureAndDropsIt() {
        IngestSchedule schedule = makeSchedule("0 0 6 * * *");
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));

        scheduler.register(schedule);
        scheduler.cancel(schedule.getId());

        verify(future).cancel(false);
        assertThat(scheduler.hasFuture(schedule.getId())).isFalse();
    }

    @Test
    void register_disabledScheduleIsNotScheduled() {
        IngestSchedule schedule = makeSchedule("0 0 6 * * *");
        schedule.setEnabled(false);

        scheduler.register(schedule);

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
        assertThat(scheduler.hasFuture(schedule.getId())).isFalse();
    }

    @Test
    void loadEnabled_registersAllEnabledSchedules() {
        IngestSchedule a = makeSchedule("0 0 6 * * *");
        IngestSchedule b = makeSchedule("0 0 12 * * *");
        when(scheduleRepository.findAllByEnabledTrue()).thenReturn(List.of(a, b));
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));

        scheduler.loadEnabled();

        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Trigger.class));
        assertThat(scheduler.hasFuture(a.getId())).isTrue();
        assertThat(scheduler.hasFuture(b.getId())).isTrue();
    }

    private static IngestSchedule makeSchedule(String cron) {
        IngestSchedule schedule = new IngestSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setName("test");
        schedule.setQuery("software engineer");
        schedule.setCountry("MY");
        schedule.setCity("");
        schedule.setRemote(false);
        schedule.setCronExpression(cron);
        schedule.setEnabled(true);
        return schedule;
    }
}
