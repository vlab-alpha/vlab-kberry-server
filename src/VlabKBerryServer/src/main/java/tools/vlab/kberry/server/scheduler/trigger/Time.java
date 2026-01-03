package tools.vlab.kberry.server.scheduler.trigger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record Time(LocalTime time) implements Trigger {

    @Override
    public boolean matches(LocalDateTime now) {
        return now.toLocalTime().withSecond(0).withNano(0)
                .equals(time.withSecond(0).withNano(0));
    }

    @JsonCreator
    public Time(@JsonProperty("time") LocalTime time) {
        this.time = time;
    }

    public static Time trigger(LocalTime time) {
        return new Time(time);
    }

}
