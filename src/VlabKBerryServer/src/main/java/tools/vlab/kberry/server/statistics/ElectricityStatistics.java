package tools.vlab.kberry.server.statistics;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.server.serviceProvider.CostWattServiceProvider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Statistik über Stromverbrauch pro Raum
 */
public class ElectricityStatistics
        extends Statistic<ElectricityStatistics.ElectricityEntry> {

    public ElectricityStatistics(Vertx vertx) {
        super(vertx, "stat/electricity.csv");
    }

    public ElectricityStatistics(Vertx vertx, String path) {
        super(vertx, path);
    }

    @Override
    protected String serializeValue(ElectricityEntry value) {
        return value.toString();
    }

    @Override
    protected ElectricityEntry deserializeValue(String line) {
        return ElectricityEntry.fromString(line);
    }

    // ───────────────────────────────
    // Verbrauchsberechnung
    // ───────────────────────────────

    private Future<Double> calculateConsumption(long from, long to, PositionPath positionPath) {
        return getValues(from, to)
                .map(values ->
                        values.values().stream()
                                .filter(e -> e.positionPath().equalsIgnoreCase(positionPath.getPath()))
                                .mapToDouble(ElectricityEntry::power)
                                .average()
                                .orElse(0.0)
                );
    }

    // ───────────────────────────────
    // Verbrauch nach Zeiträumen
    // ───────────────────────────────

    public Future<Double> getConsumptionLastHour(PositionPath positionPath) {
        return calculateConsumption(
                Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli(),
                Instant.now().toEpochMilli(),
                positionPath
        );
    }

    public Future<Double> getConsumptionLastDay(PositionPath positionPath) {
        return calculateConsumption(
                Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(),
                Instant.now().toEpochMilli(),
                positionPath
        );
    }

    public Future<Double> getConsumptionLastMonth(PositionPath positionPath) {
        return calculateConsumption(
                Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli(),
                Instant.now().toEpochMilli(),
                positionPath
        );
    }

    public Future<Double> getConsumptionLastYear(PositionPath positionPath) {
        return calculateConsumption(
                Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli(),
                Instant.now().toEpochMilli(),
                positionPath
        );
    }

    // ───────────────────────────────
    // Kostenberechnung
    // ───────────────────────────────

    public Future<Double> getCostLastDay(
            PositionPath positionPath,
            CostWattServiceProvider calculator) {

        return getConsumptionLastDay(positionPath)
                .map(calculator::calculateCost);
    }

    public Future<Double> getCostLastMonth(
            PositionPath positionPath,
            CostWattServiceProvider calculator) {

        return getConsumptionLastMonth(positionPath)
                .map(calculator::calculateCost);
    }

    public Future<Double> getCostLastYear(
            PositionPath positionPath,
            CostWattServiceProvider calculator) {

        return getConsumptionLastYear(positionPath)
                .map(calculator::calculateCost);
    }

    public Future<Double> getCostSavingDay(
            PositionPath positionPath,
            CostWattServiceProvider calculator) {

        return getConsumptionLastDay(positionPath)
                .map(calculator::calculateSavings);
    }

    public Future<Double> getCostSavingMonth(
            PositionPath positionPath,
            CostWattServiceProvider calculator) {

        return getConsumptionLastMonth(positionPath)
                .map(calculator::calculateSavings);
    }

    public Future<Double> getCostSavingYear(
            PositionPath positionPath,
            CostWattServiceProvider calculator) {

        return getConsumptionLastYear(positionPath)
                .map(calculator::calculateSavings);
    }

    // ───────────────────────────────
    // Entry
    // ───────────────────────────────

    /**
     * Eintrag: PositionPath + Verbrauch
     * (W, Wh oder kWh – je nach Speicherung)
     */
    public record ElectricityEntry(String positionPath, double power) {

        @Override
        public String toString() {
            return positionPath + "=" + power;
        }

        public static ElectricityEntry fromString(String raw) {
            String[] parts = raw.split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Ungültiger Eintrag: " + raw);
            }
            return new ElectricityEntry(parts[0], Double.parseDouble(parts[1]));
        }
    }
}