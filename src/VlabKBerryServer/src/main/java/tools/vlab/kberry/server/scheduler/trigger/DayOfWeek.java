package tools.vlab.kberry.server.scheduler.trigger;

import java.time.LocalDateTime;

public record DayOfWeek(java.time.DayOfWeek day, LocalDateTime time) implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return now.getDayOfWeek() == day && now.isEqual(time);
    }

    public static DayOfWeek trigger(java.time.DayOfWeek day, LocalDateTime time) {
        return new DayOfWeek(day, time);
    }
}
