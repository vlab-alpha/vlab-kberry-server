package tools.vlab.kberry.server.statistics;


import tools.vlab.kberry.server.serviceProvider.CostWattServiceProvider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Statistik über Stromverbrauch pro Raum
 */
public class ElectricityStatistics extends Statistic<ElectricityStatistics.ElectricityEntry> {

    public ElectricityStatistics() {
        super("stat/electricity.csv");
    }

    public ElectricityStatistics(String path) {
        super(path);
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

    private double calculateConsumption(long from, long to, String positionPath) {
        List<ElectricityEntry> entries = getValues(from, to).values().stream()
                .filter(e -> e.positionPath().equals(positionPath))
                .toList();

        if (entries.isEmpty()) {
            return 0.0;
        }

        return entries.stream()
                .mapToDouble(ElectricityEntry::power)
                .average()
                .orElse(0.0);
    }

    // Verbrauch nach Zeiträumen
    public double getConsumptionLastHour(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli();
        return calculateConsumption(from, to, positionPath);
    }

    public double getConsumptionLastDay(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        return calculateConsumption(from, to, positionPath);
    }

    public double getConsumptionLastMonth(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
        return calculateConsumption(from, to, positionPath);
    }

    public double getConsumptionLastYear(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli();
        return calculateConsumption(from, to, positionPath);
    }

    // ───────────────────────────────
    // Kostenberechnung
    // ───────────────────────────────

    public double getCostLastDay(String positionPath, CostWattServiceProvider calculator) {
        return calculator.calculateCost(getConsumptionLastDay(positionPath));
    }

    public double getCostLastMonth(String positionPath, CostWattServiceProvider calculator) {
        return calculator.calculateCost(getConsumptionLastMonth(positionPath));
    }

    public double getCostLastYear(String positionPath, CostWattServiceProvider calculator) {
        return calculator.calculateCost(getConsumptionLastYear(positionPath));
    }

    public double getCostSavingDay(String positionPath, CostWattServiceProvider calculator) {
        return calculator.calculateSavings(getConsumptionLastDay(positionPath));
    }

    public double getCostSavingMonth(String positionPath, CostWattServiceProvider calculator) {
        return calculator.calculateSavings(getConsumptionLastMonth(positionPath));
    }

    public double getCostSavingYear(String positionPath, CostWattServiceProvider calculator) {
        return calculator.calculateSavings(getConsumptionLastYear(positionPath));
    }

    /**
     * Eintrag: PositionPath + Verbrauch (in Wattstunden oder kWh, je nachdem wie du speicherst)
     *
     * @param power Verbrauchswert
     */
    public record ElectricityEntry(String positionPath, double power) {

        @Override
        public String toString() {
            return positionPath + "=" + power;
        }

        public static ElectricityEntry fromString(String raw) {
            String[] parts = raw.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Ungültiger Eintrag: " + raw);
            }
            return new ElectricityEntry(parts[0], Double.parseDouble(parts[1]));
        }
    }
}
