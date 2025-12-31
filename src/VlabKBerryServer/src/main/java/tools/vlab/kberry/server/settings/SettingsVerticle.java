package tools.vlab.kberry.server.settings;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import tools.vlab.kberry.core.PositionPath;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public abstract class SettingsVerticle<T> extends AbstractVerticle implements Settings<T>  {

    private final Path basePath;
    private final String name;

    public SettingsVerticle(String basePath, String name) {
        this.basePath = Paths.get(basePath);
        this.name = name;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        getVertx().fileSystem().mkdirs(basePath.toString())
                .onComplete(startPromise);
    }

    @Override
    public Optional<T> getSetting(PositionPath path) {
        Path filePath = buildPath(path);
        var exists = this.getVertx().fileSystem().existsBlocking(filePath.toString());
        if (exists) {
            var file = this.getVertx().fileSystem().readFileBlocking(filePath.toString());
            return Optional.of(this.toJson(file));
        }
        return Optional.empty();
    }

    @Override
    public Future<T> getSettingAsync(PositionPath path) {
        Path filePath = buildPath(path);
        return getVertx().fileSystem().exists(filePath.toString())
                .compose(exists -> {
                    if (!exists) return Future.succeededFuture(this.defaultSetting());
                    return getVertx().fileSystem().readFile(filePath.toString())
                            .map(this::toJson);
                });
    }

    public abstract T defaultSetting();

    public abstract T toJson(Buffer buffer);

    public abstract Buffer toBuffer(T setting);

    @Override
    public Future<Void> setSettingAsync(PositionPath path, T value) {
        Path filePath = buildPath(path);
        Path dirPath = filePath.getParent();
        return getVertx().fileSystem().mkdirs(dirPath.toString())
                .compose(v -> getVertx().fileSystem().writeFile(filePath.toString(), toBuffer(value)));
    }

    private Path buildPath(PositionPath path) {
        return basePath.resolve(path.toString().replace("/", Path.of("/").toString()))
                .resolve(this.name + ".json");
    }
}
