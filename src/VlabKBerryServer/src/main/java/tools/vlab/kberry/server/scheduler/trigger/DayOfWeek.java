package tools.vlab.kberry.server.scheduler.trigger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.vlab.kberry.server.scheduler.LocalTimeUtil;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record DayOfWeek(java.time.DayOfWeek day, LocalTime time) implements Trigger {

    @JsonCreator
    public DayOfWeek(@JsonProperty("time") java.time.DayOfWeek day, LocalTime time) {
        this.day = day;
        this.time = time;
    }

    @Override
    public boolean matches(LocalDateTime now) {
        return LocalTimeUtil.isSame(now, day, time);
    }

    public static DayOfWeek trigger(java.time.DayOfWeek day, LocalTime time) {
        return new DayOfWeek(day, time);
    }
}
