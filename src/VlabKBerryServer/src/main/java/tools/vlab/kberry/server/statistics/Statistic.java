package tools.vlab.kberry.server.statistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.server.log.Logger;
import tools.vlab.kberry.server.statistics.values.BooleanValue;
import tools.vlab.kberry.server.statistics.values.FloatValue;
import tools.vlab.kberry.server.statistics.values.IntValue;
import tools.vlab.kberry.server.statistics.values.PrimitiveValue;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class Statistic<T extends PrimitiveValue> extends AbstractVerticle {

    protected final String folder;
    private static final ConcurrentHashMap<String, Future<Void>> FILE_OPERATIONS = new ConcurrentHashMap<>();


    protected Statistic(String folder) {
        this.folder = folder;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        Logger.info("Folder for statistics: {}", folder);

        getFileSystem().mkdirs(folder)
                .onSuccess(v -> startPromise.complete())
                .onFailure(startPromise::fail);
    }

    // WRITE

    public Future<Void> append(PositionPath positionPath, T value) {
        var dirPath = Path.of(folder, positionPath.getLocation(), positionPath.getFloor(), positionPath.getRoom());
        var filePath = dirPath.resolve("data.log");
        String fileKey = filePath.toString();

        var colOneTS = Instant.now().toEpochMilli();
        var colTwoValue = value.serialize();
        String line = colOneTS + ";" + colTwoValue + System.lineSeparator();

        OpenOptions options = new OpenOptions()
                .setCreate(true)
                .setAppend(true)
                .setWrite(true);

        Future<Void> previous = FILE_OPERATIONS.getOrDefault(fileKey, Future.succeededFuture());

        Future<Void> current = previous.compose(v ->
                getFileSystem()
                        .mkdirs(dirPath.toString())
                        .compose(done -> getFileSystem().open(filePath.toString(), options))
                        .compose(file -> file.write(Buffer.buffer(line))
                                .compose(w -> file.close())
                                .recover(err -> file.close().compose(c -> Future.failedFuture(err)))
                        )
        );

        FILE_OPERATIONS.put(fileKey, current);

        current.onComplete(ar -> {
            if (FILE_OPERATIONS.get(fileKey) == current) {
                FILE_OPERATIONS.remove(fileKey);
            }
        });

        return current;
    }


    // READ

    public Future<List<StatisticsEntry<T>>> get(Class<T> tClass, PositionPath positionPath, LocalDateTime from, LocalDateTime to) {
        Path filePath = Path.of(
                folder,
                positionPath.getLocation(),
                positionPath.getFloor(),
                positionPath.getRoom(),
                "data.log"
        );

        FileSystem fs = getFileSystem();

        long fromTs = from.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long toTs = to.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        return fs.exists(filePath.toString())
                .compose(exists -> {
                    if (!exists) {
                        return Future.succeededFuture(List.of());
                    }

                    return fs.readFile(filePath.toString())
                            .map(buffer -> parseLines(buffer, tClass, fromTs, toTs));
                });
    }

    public Future<List<StatisticsEntry<T>>> getToday(Class<T> tClass, PositionPath positionPath) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to   = today.plusDays(1).atStartOfDay().minusNanos(1);
        return get(tClass, positionPath, from, to);
    }

    public Future<List<StatisticsEntry<T>>> getYesterday(Class<T> tClass, PositionPath positionPath) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(1).atStartOfDay();
        LocalDateTime to   = today.atStartOfDay();
        return get(tClass, positionPath, from, to);
    }

    public Future<T> getTodayAverage(Class<T> tClass, PositionPath positionPath) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to   = today.plusDays(1).atStartOfDay().minusNanos(1);
        return getAverage(tClass, positionPath, from, to);
    }

    public Future<T> getMonthAverage(Class<T> tClass, PositionPath positionPath) {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);

        LocalDateTime from = firstDay.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        return getAverage(tClass, positionPath, from, to);
    }

    public Future<T> getAverage(Class<T> tClass, PositionPath positionPath, LocalDateTime from, LocalDateTime to) {
        return get(tClass, positionPath, from, to)
                .map(list -> {
                    if (list.isEmpty()) return null;

                    double avg = getAvg(list);
                    if (tClass == IntValue.class) return tClass.cast(new IntValue((int)Math.round(avg)));
                    else if (tClass == FloatValue.class) return tClass.cast(new FloatValue((float)avg));
                    else if (tClass == BooleanValue.class) return tClass.cast(new BooleanValue(avg >= 0.5));
                    else throw new IllegalArgumentException("Unsupported type: " + tClass);
                });
    }


    private List<StatisticsEntry<T>> parseLines(Buffer buffer, Class<T> tClass, long fromTs, long toTs) {
        List<StatisticsEntry<T>> result = new ArrayList<>();

        for (String line : buffer.toString().split("\\R")) {
            processLine(line, tClass, fromTs, toTs, result);
        }

        return result;
    }

    private void processLine(String line, Class<T> tClass, long fromTs, long toTs, List<StatisticsEntry<T>> result) {
        if (line.isBlank()) return;

        String[] parts = line.split(";", 2);
        if (parts.length != 2) return;

        long ts;
        try {
            ts = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return;
        }

        if (ts < fromTs || ts > toTs) return;

        try {
            T value = deserialize(parts[1], tClass);
            result.add(new StatisticsEntry<>(ts, value));
        } catch (Exception e) {
            // Ungültiger Wert
        }
    }


    private static <T extends PrimitiveValue> double getAvg(List<StatisticsEntry<T>> list) {
        double sum = 0;
        for (StatisticsEntry<T> entry : list) {
            PrimitiveValue val = entry.value();
            if (val instanceof IntValue(int value)) {
                sum += value;
            } else if (val instanceof FloatValue(float value)) {
                sum += value;
            } else if (val instanceof BooleanValue(boolean value)) {
                sum += value ? 1 : 0;
            }
        }
        return sum / list.size();
    }


    @SuppressWarnings("unchecked")
    private T deserialize(String raw, Class<T> type) {

        if (type == IntValue.class) {
            return (T) new IntValue(Integer.parseInt(raw));
        }

        if (type == FloatValue.class) {
            return (T) new FloatValue(Float.parseFloat(raw));
        }

        if (type == BooleanValue.class) {
            return (T) new BooleanValue(Boolean.parseBoolean(raw));
        }

        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private FileSystem getFileSystem() {
        return this.getVertx().fileSystem();
    }

}
