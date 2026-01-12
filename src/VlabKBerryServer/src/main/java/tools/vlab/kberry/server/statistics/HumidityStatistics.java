package tools.vlab.kberry.server.statistics;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import tools.vlab.kberry.core.PositionPath;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class HumidityStatistics
        extends Statistic<HumidityStatistics.HumidityEntry> {

    public HumidityStatistics(Vertx vertx) {
        super(vertx, "stat/humidity.csv");
    }

    @Override
    protected String serializeValue(HumidityEntry value) {
        return value.toString();
    }

    @Override
    protected HumidityEntry deserializeValue(String raw) {
        return HumidityEntry.fromString(raw);
    }

    public Future<Double> calculateAverage(long from, long to, PositionPath positionPath) {
        return getValues(from, to)
                .map(values -> values.values().stream()
                        .filter(e -> e.positionPath.toLowerCase().startsWith(positionPath.getPath().toLowerCase()))
                        .mapToDouble(HumidityEntry::humidity)
                        .average()
                        .orElse(Double.NaN));
    }

    public Future<Float> getCurrentHumidity(PositionPath positionPath) {
        return getValues(0, Instant.now().toEpochMilli())
                .map(values -> values.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getKey(), a.getKey()))
                        .map(Map.Entry::getValue)
                        .filter(e -> e.positionPath().equalsIgnoreCase(positionPath.getPath()))
                        .map(HumidityEntry::humidity)
                        .findFirst()
                        .orElse(null)
                );
    }

    public Future<Double> getAverageLastHour(PositionPath positionPath) {
        return calculateAverage(
                Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli(),
                Instant.now().toEpochMilli(),
                positionPath
        );
    }

    public Future<Double> getAverageLastDay(PositionPath positionPath) {
        return calculateAverage(
                Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(),
                Instant.now().toEpochMilli(),
                positionPath
        );
    }

    public Future<Double> getAverageLastMonth(PositionPath positionPath) {
        return calculateAverage(
                Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli(),
                Instant.now().toEpochMilli(),
                positionPath
        );
    }

    public Future<Double> getAverageLastYear(PositionPath positionPath) {
        return calculateAverage(
                Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli(),
                Instant.now().toEpochMilli(),
                positionPath
        );
    }

    public Future<Boolean> isEmpty(PositionPath positionPath) {
        return getValuesLastHour()
                .map(values -> values.values().stream()
                        .noneMatch(v -> v.positionPath.toLowerCase()
                                .startsWith(positionPath.getPath().toLowerCase())));
    }

    public record HumidityEntry(String positionPath, float humidity) {

        @Override
        public String toString() {
            return positionPath + "=" + humidity;
        }

        public static HumidityEntry fromString(String s) {
            String[] parts = s.split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid Entry: " + s);
            }
            return new HumidityEntry(parts[0], Float.parseFloat(parts[1]));
        }
    }
}