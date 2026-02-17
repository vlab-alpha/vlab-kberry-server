package tools.vlab.kberry.server.statistics;

import io.vertx.core.json.JsonObject;
import tools.vlab.kberry.server.statistics.values.PrimitiveValue;

import java.util.Objects;

public record StatisticsEntry<T extends PrimitiveValue>(
        long timestamp,
        T value
) {

    public JsonObject toJson() {
        var val = value.serialize();
        return new JsonObject()
                .put("timestamp", timestamp)
                .put("value", Objects.requireNonNullElse(val, 0));
    }

}
