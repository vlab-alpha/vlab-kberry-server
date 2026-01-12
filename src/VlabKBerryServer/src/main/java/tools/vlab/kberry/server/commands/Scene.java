package tools.vlab.kberry.server.commands;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import tools.vlab.kberry.core.PositionPath;

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

    public String getTopic() {
        return String.join("/",
                this.getPositionPath().getLocation(),
                this.getPositionPath().getFloor(),
                this.getPositionPath().getRoom(),
                this.getName().trim()
        );
    }

    public abstract PositionPath getPositionPath();

    public abstract String getIcon();

    public abstract String getName();
}
