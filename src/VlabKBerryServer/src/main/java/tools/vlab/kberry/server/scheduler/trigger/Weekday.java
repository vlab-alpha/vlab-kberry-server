package tools.vlab.kberry.server.scheduler.trigger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.vlab.kberry.server.scheduler.LocalTimeUtil;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record Weekday(LocalTime time) implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return LocalTimeUtil.isWeekday(now) && LocalTimeUtil.isSame(now, time);
    }

    @JsonCreator
    public Weekday(@JsonProperty("time") LocalTime time) {
        this.time = time;
    }

    public static Weekday trigger(LocalTime time) {
        return new Weekday(time);
    }
}
