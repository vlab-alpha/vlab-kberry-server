package tools.vlab.kberry.server.statistics;

import tools.vlab.kberry.core.PositionPath;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TemperaturStatistics extends Statistic<TemperaturStatistics.TemperatureEntry> {

    public TemperaturStatistics() throws IOException {
        super("stat/temperature.csv");
    }

    public TemperaturStatistics(String filePath) throws IOException {
        super(filePath);
    }

    @Override
    protected String serializeValue(TemperatureEntry value) {
        return value.toString();
    }

    @Override
    protected TemperatureEntry deserializeValue(String line) {
        return TemperatureEntry.fromString(line);
    }

    /**
     * Durchschnittstemperatur im Zeitraum
     */
    public double calculateAverage(long from, long to, String positionPath) {
        List<TemperatureEntry> entries = getValues(from, to).values().stream().toList();

        double sum = 0.0;
        int count = 0;

        for (TemperatureEntry e : entries) {
            if (!e.positionPath().equalsIgnoreCase(positionPath)) continue;
            sum += e.temperature();
            count++;
        }

        return (count == 0) ? Double.NaN : sum / count;
    }

    public Double getCurrentTemperature(String positionPath) {
        List<TemperatureEntry> entries = getValues(0, Instant.now().toEpochMilli()).values().stream().toList(); // alle Einträge
        for (int i = entries.size() - 1; i >= 0; i--) {
            TemperatureEntry e = entries.get(i);
            if (e.positionPath().equals(positionPath)) {
                return e.temperature();
            }
        }
        return null; // keine Daten vorhanden
    }

    /**
     * Durchschnitt letzte Stunde
     */
    public double getAverageLastHour(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }

    /**
     * Durchschnitt letzter Tag
     */
    public double getAverageLastDay(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }


    /**
     * Durchschnitt letzter Monat (30 Tage)
     */
    public double getAverageLastMonth(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }

    /**
     * Durchschnitt letztes Jahr (365 Tage)
     */
    public double getAverageLastYear(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }


    public boolean isEmpty(String positionPath) {
        return getValuesLastHour().values().stream()
                .anyMatch(val -> val.positionPath().equalsIgnoreCase(positionPath));
    }

    public Map<Long, Double> getValuesLastDay(PositionPath positionPath) {
        return getValuesLastHour().entrySet().stream()
                .filter(entry -> entry.getValue().positionPath.equalsIgnoreCase(positionPath.toString()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        keyValue -> keyValue.getValue().temperature,
                        (existing, replacement) -> replacement
                ));
    }

    /**
     * Eintrag: Raum + Temperaturwert
     */
    public record TemperatureEntry(String positionPath, double temperature) {

        @Override
        public String toString() {
            return positionPath + "=" + temperature;
        }

        public static TemperatureEntry fromString(String s) {
            String[] parts = s.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Ungültiger Eintrag: " + s);
            }
            return new TemperatureEntry(parts[0], Double.parseDouble(parts[1]));
        }
    }
}
