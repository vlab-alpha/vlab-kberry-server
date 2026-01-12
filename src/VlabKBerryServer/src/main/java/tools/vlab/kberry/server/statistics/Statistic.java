package tools.vlab.kberry.server.statistics;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Statistic<T> {

    protected final Vertx vertx;
    protected final String filePath;
    protected final FileSystem fs;

    protected Statistic(Vertx vertx, String filePath) {
        this.vertx = vertx;
        this.filePath = filePath;
        this.fs = vertx.fileSystem();
    }

    public Future<Void> start() {
        Path dir = Path.of(filePath).getParent(); // z.B. "stat"

        return fs.exists(dir.toString())
                .compose(exists -> exists ? Future.succeededFuture() : fs.mkdirs(dir.toString()))
                .compose(v -> fs.exists(filePath))
                .compose(exists -> exists ? Future.succeededFuture() : fs.createFile(filePath));
    }

    protected abstract String serializeValue(T value);

    protected abstract T deserializeValue(String raw);

    public Future<Void> append(T value) {
        return append(Instant.now().toEpochMilli(), value);
    }

    public Future<Void> append(long timestamp, T value) {
        String line = timestamp + ";" + serializeValue(value) + "\n";
        return fs.open(filePath,
                        new io.vertx.core.file.OpenOptions().setAppend(true).setCreate(true))
                .compose(file -> file.write(Buffer.buffer(line))
                        .onComplete(v -> file.close()));
    }

    public Future<Map<Long, T>> getValuesLastMonth() {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
        return getValues(from, to);
    }

    public Future<Map<Long, T>> getValuesLastYear() {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli();
        return getValues(from, to);
    }

    public Future<Map<Long, T>> getValuesLastHour() {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli();
        return getValues(from, to);
    }

    public Future<Map<Long, T>> getValuesLastDay() {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        return getValues(from, to);
    }

    public Future<Map<Long, T>> getValues(long from, long to) {
        return fs.readFile(filePath)
                .map(buffer -> {
                    Map<Long, T> result = new LinkedHashMap<>();

                    buffer.toString()
                            .lines()
                            .map(line -> line.split(";", 2))
                            .filter(parts -> parts.length == 2)
                            .forEach(parts -> {
                                long ts = Long.parseLong(parts[0]);
                                if (ts >= from && ts <= to) {
                                    result.put(ts, deserializeValue(parts[1]));
                                }
                            });

                    return result;
                });
    }


    public Future<Void> clean(long from) {
        return fs.readFile(filePath)
                .compose(buffer -> {
                    StringBuilder sb = new StringBuilder();

                    buffer.toString()
                            .lines()
                            .map(line -> line.split(";", 2))
                            .filter(parts -> parts.length == 2)
                            .forEach(parts -> {
                                long ts = Long.parseLong(parts[0]);
                                if (ts >= from) {
                                    sb.append(parts[0]).append(";")
                                            .append(parts[1]).append("\n");
                                }
                            });

                    return fs.writeFile(filePath, Buffer.buffer(sb.toString()));
                });
    }

    public Future<Void> cleanOlderThanDays(int days) {
        long cutoff = Instant.now()
                .minus(days, ChronoUnit.DAYS)
                .toEpochMilli();
        return clean(cutoff);
    }
}
