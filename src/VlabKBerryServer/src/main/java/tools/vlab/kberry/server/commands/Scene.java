package tools.vlab.kberry.server.commands;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

public abstract class Scene extends Command {

    @Override
    public Future<Optional<JsonObject>> execute(JsonObject message) {
        executeScene(message);
        return Future.succeededFuture(Optional.empty());
    }

    public abstract void executeScene(JsonObject message);

    @Override
    public String getMqttTopic() {
        return "scene/" + topic().getTopic();
    }
}
