package tools.vlab.kberry.server.statistics;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import tools.vlab.kberry.core.PositionPath;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public class TemperaturStatistics
        extends Statistic<TemperaturStatistics.TemperatureEntry> {

    public TemperaturStatistics(Vertx vertx) {
        super(vertx, "stat/temperature.csv");
    }

    @Override
    protected String serializeValue(TemperatureEntry value) {
        return value.toString();
    }

    @Override
    protected TemperatureEntry deserializeValue(String line) {
        return TemperatureEntry.fromString(line);
    }

    public Future<Double> calculateAverage(long from, long to, PositionPath positionPath) {
        return getValues(from, to)
                .map(values -> {
                    OptionalDouble avg = values.values().stream()
                            .filter(e -> e.positionPath().equalsIgnoreCase(positionPath.getPath()))
                            .mapToDouble(TemperatureEntry::temperature)
                            .average();

                    return avg.isPresent() ? avg.getAsDouble() : Double.NaN;
                });
    }

    public Future<Double> getCurrentTemperature(PositionPath positionPath) {
        return getValues(0, Instant.now().toEpochMilli())
                .map(values -> values.values().stream()
                        .filter(e -> e.positionPath().equalsIgnoreCase(positionPath.getPath()))
                        .reduce((first, second) -> second) // letzter Eintrag
                        .map(TemperatureEntry::temperature)
                        .orElse(null));
    }

    public Future<Double> getAverageLastHour(PositionPath positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }

    public Future<Double> getAverageLastDay(PositionPath positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }

    public Future<Double> getAverageLastMonth(PositionPath positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }

    public Future<Double> getAverageLastYear(PositionPath positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }

    public Future<Boolean> hasValuesLastHour(PositionPath positionPath) {
        return getValuesLastHour()
                .map(values -> values.values().stream()
                        .anyMatch(v -> v.positionPath().equalsIgnoreCase(positionPath.getPath())));
    }

    public Future<Map<Long, Double>> getValuesLastDay(PositionPath positionPath) {
        return getValuesLastDay()
                .map(values -> values.entrySet().stream()
                        .filter(e -> e.getValue().positionPath()
                                .equalsIgnoreCase(positionPath.getPath()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().temperature(),
                                (a, b) -> b,
                                LinkedHashMap::new
                        )));
    }

    public record TemperatureEntry(String positionPath, double temperature) {

        @Override
        public String toString() {
            return positionPath + "=" + temperature;
        }

        public static TemperatureEntry fromString(String s) {
            String[] parts = s.split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Ungültiger Eintrag: " + s);
            }
            return new TemperatureEntry(parts[0], Double.parseDouble(parts[1]));
        }
    }
}