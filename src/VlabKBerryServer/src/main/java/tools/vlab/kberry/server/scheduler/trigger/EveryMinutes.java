package tools.vlab.kberry.server.scheduler.trigger;

import java.time.LocalDateTime;

public record EveryMinutes() implements Trigger  {

    @Override
    public boolean matches(LocalDateTime now) {
        return now.getSecond() == 0;
    }

    public static EveryMinutes trigger() {
        return new EveryMinutes();
    }
}
