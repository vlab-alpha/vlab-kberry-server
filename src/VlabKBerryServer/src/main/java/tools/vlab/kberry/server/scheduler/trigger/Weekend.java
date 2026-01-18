package tools.vlab.kberry.server.scheduler.trigger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.vlab.kberry.server.scheduler.LocalTimeUtil;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record Weekend(LocalTime time) implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return LocalTimeUtil.isWeekend(now) && LocalTimeUtil.isSame(now, time);
    }

    @JsonCreator
    public Weekend(@JsonProperty("time") LocalTime time) {
        this.time = time;
    }

    public static Weekend trigger(LocalTime time) {
        return new Weekend(time);
    }
}
