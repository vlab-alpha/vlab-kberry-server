package tools.vlab.kberry.server.scheduler.trigger;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public record DayOfWeekTrigger(DayOfWeek day, Trigger delegate) implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return now.getDayOfWeek() == day && delegate.matches(now);
    }
}
