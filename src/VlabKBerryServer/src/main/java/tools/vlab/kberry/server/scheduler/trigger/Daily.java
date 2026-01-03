package tools.vlab.kberry.server.scheduler.trigger;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record Daily(LocalTime time) implements Trigger  {

    @Override
    public boolean matches(LocalDateTime now) {
        return now.toLocalTime().equals(time);
    }

    public static Daily trigger(LocalTime time) {
        return new Daily(time);
    }
}
