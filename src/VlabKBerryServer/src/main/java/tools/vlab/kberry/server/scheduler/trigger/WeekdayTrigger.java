package tools.vlab.kberry.server.scheduler.trigger;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public record WeekdayTrigger(Trigger delegate) implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return !isWeekend(now.getDayOfWeek()) && delegate.matches(now);
    }

    private boolean isWeekend(DayOfWeek day) {
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
