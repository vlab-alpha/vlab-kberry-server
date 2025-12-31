package tools.vlab.kberry.server.statistics;


import tools.vlab.kberry.core.PositionPath;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class VOCStatistics extends Statistic<VOCStatistics.VOCEntry> {

    public VOCStatistics() {
        super("stat/voc.csv");
    }

    public VOCStatistics(String filePath) {
        super(filePath);
    }

    @Override
    protected String serializeValue(VOCEntry value) {
        return value.toString();
    }

    @Override
    protected VOCEntry deserializeValue(String raw) {
        return VOCEntry.fromString(raw);
    }

    public double calculateAverage(long from, long to, String positionPath) {
        List<VOCEntry> entries = getValues(from, to).values().stream().toList();

        double sum = 0.0;
        int count = 0;

        for (VOCEntry e : entries) {
            // Alle Einträge akzeptieren, die mit dem Such-Pfad beginnen
            if (!e.positionPath().toLowerCase().startsWith(positionPath.toLowerCase())) continue;
            sum += e.co2();
            count++;
        }

        return (count == 0) ? Double.NaN : sum / count;
    }

    public double getAverageLastHourByFloor(PositionPath positionPath) {
        return getAverageLastHour(String.join("/", positionPath.getLocation(), positionPath.getFloor()));
    }

    public double getAverageLastDayByFloor(PositionPath positionPath) {
        return getAverageLastDay(String.join("/", positionPath.getLocation(), positionPath.getFloor()));
    }

    public double getAverageLastHourByRoom(PositionPath positionPath) {
        return getAverageLastHour(String.join("/", positionPath.getLocation(), positionPath.getFloor(), positionPath.getRoom()));
    }

    public Double getCurrentCo2(String positionPath) {
        List<VOCEntry> entries = getValues(0, Instant.now().toEpochMilli()).values().stream().toList(); // alle Einträge
        for (int i = entries.size() - 1; i >= 0; i--) {
            VOCEntry e = entries.get(i);
            if (e.positionPath().equals(positionPath)) {
                return e.co2();
            }
        }
        return null; // keine Daten vorhanden
    }

    public double getAverageLastHour(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }

    public double getAverageLastDay(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }

    public double getAverageLastMonth(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }

    public double getAverageLastYear(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli();
        return calculateAverage(from, to, positionPath);
    }

    public boolean isEmpty(String positionPath) {
        return getValuesLastHour().values().stream()
                .noneMatch(val -> val.positionPath().equalsIgnoreCase(positionPath));
    }

    public IndoorClimate getIndoorClimate(String positionPath) {
        Double co2 = getCurrentCo2(positionPath);
        if (co2 == null) return IndoorClimate.NORMAL; // keine Daten → neutral

        if (co2 < 600) return IndoorClimate.GOOD;
        if (co2 < 1000) return IndoorClimate.NORMAL;
        if (co2 < 1500) return IndoorClimate.WARNING;
        return IndoorClimate.BAD;
    }

    public record VOCEntry(String positionPath, double co2) {

        @Override
        public String toString() {
            return positionPath + "=" + co2;
        }

        public static VOCEntry fromString(String raw) {
            String[] parts = raw.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Ungültiger Eintrag: " + raw);
            }
            return new VOCEntry(parts[0], Double.parseDouble(parts[1]));
        }
    }

}
