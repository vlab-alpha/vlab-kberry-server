package tools.vlab.kberry.server.statistics;

import tools.vlab.kberry.core.PositionPath;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class HumidityStatistics extends Statistic<HumidityStatistics.HumidityEntry> {

    public HumidityStatistics() {
        super("stat/humidity.csv");
    }

    @Override
    protected String serializeValue(HumidityEntry value) {
        return value.toString();
    }

    @Override
    protected HumidityEntry deserializeValue(String raw) {
        return HumidityEntry.fromString(raw);
    }

    public double getAverageLastHourByFloor(PositionPath positionPath) {
        return getAverageLastHour(String.join("/", positionPath.getLocation()));
    }

    public double getAverageLastDayByFloor(PositionPath positionPath) {
        return getAverageLastDay(String.join("/", positionPath.getLocation(), positionPath.getFloor()));
    }

    public double getAverageLastHourByRoom(PositionPath positionPath) {
        return getAverageLastHour(String.join("/", positionPath.getLocation(), positionPath.getFloor(), positionPath.getRoom()));
    }

    /**
     * Durchschnittlicher Feuchtigkeitswert im Zeitraum.
     * positionPath kann Raum, Stockwerk oder Haus sein (Prefix-Matching).
     */
    public double calculateAverage(long from, long to, String positionPath) {
        List<HumidityEntry> entries = getValues(from, to).values().stream().toList();

        double sum = 0.0;
        int count = 0;

        for (HumidityEntry e : entries) {
            if (!e.positionPath().toLowerCase().startsWith(positionPath.toLowerCase())) continue;
            sum += e.humidity();
            count++;
        }

        return (count == 0) ? Double.NaN : sum / count;
    }

    public Float getCurrentHumidity(String positionPath) {
        List<HumidityEntry> entries = getValues(0, Instant.now().toEpochMilli()).values().stream().toList(); // alle Einträge
        for (int i = entries.size() - 1; i >= 0; i--) {
            HumidityEntry e = entries.get(i);
            if (e.positionPath().toLowerCase().startsWith(positionPath.toLowerCase())) {
                return e.humidity();
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
                .noneMatch(val -> val.positionPath().toLowerCase().startsWith(positionPath.toLowerCase()));
    }

    /**
     * Eintrag: Raum + Luftfeuchtigkeit
     */
    public record HumidityEntry(String positionPath, float humidity) {

        @Override
        public String toString() {
            return positionPath + "=" + humidity;
        }

        public static HumidityEntry fromString(String s) {
            String[] parts = s.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid Entry: " + s);
            }
            return new HumidityEntry(parts[0], Float.parseFloat(parts[1]));
        }
    }
}
