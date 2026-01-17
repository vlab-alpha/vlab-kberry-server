package tools.vlab.kberry.server.scheduler.trigger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record Daily(LocalTime time) implements Trigger  {

    @JsonCreator
    public Daily(@JsonProperty("time") LocalTime time) {
        this.time = time;
    }

    @Override
    public boolean matches(LocalDateTime now) {
        return now.toLocalTime().equals(time);
    }

    public static Daily trigger(LocalTime time) {
        return new Daily(time);
    }
}
