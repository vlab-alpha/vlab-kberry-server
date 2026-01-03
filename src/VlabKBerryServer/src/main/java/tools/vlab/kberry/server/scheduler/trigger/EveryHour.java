package tools.vlab.kberry.server.scheduler.trigger;

import java.time.LocalDateTime;

public record EveryHour() implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return now.getMinute() == 0 && now.getSecond() == 0;
    }

    public static EveryHour trigger() {
        return new EveryHour();
    }
}
