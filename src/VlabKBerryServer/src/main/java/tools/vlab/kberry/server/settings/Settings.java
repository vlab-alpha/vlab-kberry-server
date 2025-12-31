package tools.vlab.kberry.server.settings;

import io.vertx.core.Future;
import tools.vlab.kberry.core.PositionPath;

import java.util.Optional;

public interface Settings <T> {

    Future<T> getSettingAsync(PositionPath path);

    Future<Void> setSettingAsync(PositionPath path, T value);

    Optional<T> getSetting(PositionPath path);
}
