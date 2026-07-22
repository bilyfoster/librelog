package com.onelpro.librelog.rumble.service;

import com.onelpro.librelog.schedule.ScheduleItem;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class JazzHandoffService {

    private final Clock clock;

    public JazzHandoffService() {
        this(Clock.systemDefaultZone());
    }

    JazzHandoffService(Clock clock) {
        this.clock = clock;
    }

    public void assertSafeHandoffDate(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("targetDate is required");
        }
        LocalDate today = LocalDate.now(clock);
        if (targetDate.isBefore(today.plusDays(1))) {
            throw new IllegalArgumentException(
                    "CRITICAL: Modifications to active 24-hour playout buffers are blocked to ensure system stability.");
        }
    }

    public HandoffPlan commitLogToJazz(LocalDate targetDate, List<ScheduleItem> compiledLogEntries) {
        assertSafeHandoffDate(targetDate);
        int entryCount = compiledLogEntries == null ? 0 : compiledLogEntries.size();
        return new HandoffPlan(targetDate, entryCount);
    }

    public record HandoffPlan(LocalDate targetDate, int entryCount) {
    }
}
