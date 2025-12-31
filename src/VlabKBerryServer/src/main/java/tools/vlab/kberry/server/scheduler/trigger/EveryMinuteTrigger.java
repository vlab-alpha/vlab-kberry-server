package tools.vlab.kberry.server.scheduler.trigger;

import java.time.LocalDateTime;

public record EveryMinuteTrigger() implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return now.getSecond() == 0;
    }
}
