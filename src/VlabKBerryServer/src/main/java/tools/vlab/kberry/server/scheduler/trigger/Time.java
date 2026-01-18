package tools.vlab.kberry.server.scheduler.trigger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.vlab.kberry.server.scheduler.LocalTimeUtil;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record Time(LocalTime time) implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return LocalTimeUtil.isSame(now, time);
    }

    @JsonCreator
    public Time(@JsonProperty("time") LocalTime time) {
        this.time = time;
    }

    public static Time trigger(LocalTime time) {
        return new Time(time);
    }

}
