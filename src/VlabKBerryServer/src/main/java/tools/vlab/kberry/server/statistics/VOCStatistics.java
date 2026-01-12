package tools.vlab.kberry.server.statistics;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import tools.vlab.kberry.core.PositionPath;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.OptionalDouble;

public class VOCStatistics extends Statistic<VOCStatistics.VOCEntry> {

    public VOCStatistics(Vertx vertx) {
        super(vertx, "stat/voc.csv");
    }

    @Override
    protected String serializeValue(VOCEntry value) {
        return value.toString();
    }

    @Override
    protected VOCEntry deserializeValue(String raw) {
        return VOCEntry.fromString(raw);
    }

    /**
     * Durchschnittlicher CO2-Wert im Zeitraum.
     * Filtert nach Präfix (Raum, Stockwerk, Gebäude)
     */
    public Future<Double> calculateAverage(long from, long to, PositionPath positionPath) {
        String pathPrefix = positionPath.getPath().toLowerCase();
        return getValues(from, to)
                .map(values -> {
                    OptionalDouble avg = values.values().stream()
                            .filter(e -> e.positionPath().toLowerCase().startsWith(pathPrefix))
                            .mapToDouble(VOCEntry::co2)
                            .average();
                    return avg.isPresent() ? avg.getAsDouble() : Double.NaN;
                });
    }

    /**
     * Letzter verfügbarer CO2-Wert für den exakten Pfad
     */
    public Future<Double> getCurrentCo2(PositionPath positionPath) {
        String path = positionPath.getPath();
        return getValues(0, Instant.now().toEpochMilli())
                .map(values -> values.values().stream()
                        .filter(e -> e.positionPath.equalsIgnoreCase(path))
                        .reduce((first, second) -> second) // letzter Eintrag
                        .map(VOCEntry::co2)
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
        String path = positionPath.getPath();
        return getValuesLastHour()
                .map(values -> values.values().stream()
                        .anyMatch(v -> v.positionPath.equalsIgnoreCase(path)));
    }

    public Future<IndoorClimate> getIndoorClimate(PositionPath positionPath) {
        return getCurrentCo2(positionPath)
                .map(co2 -> {
                    if (co2 == null) return IndoorClimate.NORMAL;
                    if (co2 < 600) return IndoorClimate.GOOD;
                    if (co2 < 1000) return IndoorClimate.NORMAL;
                    if (co2 < 1500) return IndoorClimate.WARNING;
                    return IndoorClimate.BAD;
                });
    }

    public record VOCEntry(String positionPath, double co2) {

        @Override
        public String toString() {
            return positionPath + "=" + co2;
        }

        public static VOCEntry fromString(String raw) {
            String[] parts = raw.split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Ungültiger Eintrag: " + raw);
            }
            return new VOCEntry(parts[0], Double.parseDouble(parts[1]));
        }
    }
}