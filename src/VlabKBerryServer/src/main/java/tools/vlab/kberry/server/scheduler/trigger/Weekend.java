package tools.vlab.kberry.server.scheduler.trigger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record Weekend(LocalTime time) implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return isWeekend(now.getDayOfWeek()) && now.toLocalTime().equals(time);
    }

    @JsonCreator
    public Weekend(@JsonProperty("time") LocalTime time) {
        this.time = time;
    }

    private boolean isWeekend(DayOfWeek day) {
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public static Weekend trigger(LocalTime time) {
        return new Weekend(time);
    }
}
