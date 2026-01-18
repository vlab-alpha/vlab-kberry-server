package tools.vlab.kberry.server.scheduler.trigger;

import java.time.LocalDateTime;

public record EverySecond() implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return true;
    }

    public static EverySecond trigger() {
        return new EverySecond();
    }
}
