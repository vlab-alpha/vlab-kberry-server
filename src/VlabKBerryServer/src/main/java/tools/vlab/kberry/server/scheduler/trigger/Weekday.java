package tools.vlab.kberry.server.scheduler.trigger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record Weekday(LocalTime time) implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return !isWeekend(now.getDayOfWeek()) && now.toLocalTime().equals(time);
    }

    @JsonCreator
    public Weekday(@JsonProperty("time") LocalTime time) {
        this.time = time;
    }

    private boolean isWeekend(DayOfWeek day) {
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public static Weekday trigger(LocalTime time) {
        return new Weekday(time);
    }
}
