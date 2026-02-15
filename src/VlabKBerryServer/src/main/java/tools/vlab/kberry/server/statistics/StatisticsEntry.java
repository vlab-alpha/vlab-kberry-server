package tools.vlab.kberry.server.statistics;

import io.vertx.core.json.JsonObject;
import tools.vlab.kberry.server.statistics.values.PrimitiveValue;

public record StatisticsEntry<T extends PrimitiveValue>(
        long timestamp,
        T value
) {

    public JsonObject toJson() {
        return new JsonObject()
                .put("timestamp", timestamp)
                .put("value", value.serialize());
    }

}
