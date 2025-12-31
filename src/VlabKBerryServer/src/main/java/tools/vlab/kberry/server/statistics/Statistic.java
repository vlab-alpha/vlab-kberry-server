package tools.vlab.kberry.server.statistics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Statistic<T> {

    private final Path filePath;

    protected Statistic(String fileName) {
        this.filePath = Paths.get(fileName);
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Konnte Statistik-Datei nicht erstellen: " + filePath, e);
        }
    }

    protected abstract String serializeValue(T value);

    protected abstract T deserializeValue(String raw);

    public void append(T value) {
        append(Instant.now().toEpochMilli(), value);
    }

    public void append(long timestamp, T value) {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                StandardOpenOption.APPEND)) {
            writer.write(timestamp + ";" + serializeValue(value));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Schreiben in Statistik-Datei", e);
        }
    }

    public Map<Long, T> getValuesLastHour() {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli();
        return getValues(from, to);
    }

    public Map<Long, T> getValuesLastDay() {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        return getValues(from, to);
    }

    public Map<Long, T> getValuesLastMonth() {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
        return getValues(from, to);
    }

    public Map<Long, T> getValuesLastYear() {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli();
        return getValues(from, to);
    }

    public Map<Long, T> getValues(long from, long to) {
        Map<Long, T> result = new LinkedHashMap<>(); // behält Einfügereihenfolge
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            reader.lines()
                    .map(line -> line.split(";", 2))
                    .filter(parts -> parts.length == 2)
                    .forEach(parts -> {
                        long ts = Long.parseLong(parts[0]);
                        if (ts >= from && ts <= to) {
                            result.put(ts, deserializeValue(parts[1]));
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Lesen aus Statistik-Datei", e);
        }
        return result;
    }

    public void cleanOlderThanDays(int days) {
        long cutoff = Instant.now()
                .minus(days, ChronoUnit.DAYS)
                .toEpochMilli();
        clean(cutoff);
    }

    public void clean(long from) {
        try {
            Map<Long, String> remaining = new LinkedHashMap<>();
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                reader.lines()
                        .map(line -> line.split(";", 2))
                        .filter(parts -> parts.length == 2)
                        .forEach(parts -> {
                            long ts = Long.parseLong(parts[0]);
                            if (ts >= from) {
                                remaining.put(ts, parts[1]);
                            }
                        });
            }

            try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                for (Map.Entry<Long, String> e : remaining.entrySet()) {
                    writer.write(e.getKey() + ";" + e.getValue());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Clean file error!", e);
        }
    }
}
