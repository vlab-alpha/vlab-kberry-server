package tools.vlab.kberry.server.statistics;

import tools.vlab.kberry.core.PositionPath;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class PresentStatistics extends Statistic<PresentStatistics.PresenceEntry> {

    public PresentStatistics() {
        super("stat/presence.csv");
    }

    public PresentStatistics(String filePath) {
        super(filePath);
    }

    @Override
    protected String serializeValue(PresenceEntry value) {
        return value.toString();
    }

    @Override
    protected PresenceEntry deserializeValue(String line) {
        return PresenceEntry.fromString(line);
    }

    /**
     * Nutzungsquote in Prozent über ein Zeitfenster (0–100).
     */
    public double calculateUsage(long from, long to, String positionPath) {
        List<PresenceEntry> entries = getValues(from, to).values().stream().toList();

        if (entries.isEmpty()) {
            return 0.0;
        }

        long totalCount = 0;
        long presentCount = 0;

        for (PresenceEntry e : entries) {
            if (!e.positionPath().equals(positionPath)) continue;
            totalCount++;
            if (e.present()) {
                presentCount++;
            }
        }

        if (totalCount == 0) {
            return 0.0;
        }

        return (presentCount * 100.0) / totalCount;
    }

    /**
     * Nutzungsquote letzten Tag
     */
    public double getUsageLastDay(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        return calculateUsage(from, to, positionPath);
    }

    /**
     * Nutzungsquote letzten Monat (30 Tage)
     */
    public double getUsageLastMonth(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
        return calculateUsage(from, to, positionPath);
    }

    /**
     * Nutzungsquote letztes Jahr (365 Tage)
     */
    public double getUsageLastYear(String positionPath) {
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli();
        return calculateUsage(from, to, positionPath);
    }

    public long getLastPresenceMinutes(PositionPath positionPath) {
        return this.getLastPresenceMinutes(positionPath.toString());
    }

    /**
     * Letzte Präsenz in Minuten.
     * Gibt -1 zurück, wenn keine Präsenz gefunden wurde.
     */
    public long getLastPresenceMinutes(String positionPath) {
        Long ts = getLastPresenceTimestamp(positionPath);
        if (ts == null) return -1;
        return ChronoUnit.MINUTES.between(Instant.ofEpochMilli(ts), Instant.now());
    }

    /**
     * Letzte Präsenz in Stunden
     */
    public long getLastPresenceHours(String positionPath) {
        Long ts = getLastPresenceTimestamp(positionPath);
        if (ts == null) return -1;
        return ChronoUnit.HOURS.between(Instant.ofEpochMilli(ts), Instant.now());
    }

    /**
     * Letzte Präsenz in Tagen
     */
    public long getLastPresenceDays(String positionPath) {
        Long ts = getLastPresenceTimestamp(positionPath);
        if (ts == null) return -1;
        return ChronoUnit.DAYS.between(Instant.ofEpochMilli(ts), Instant.now());
    }

    /**
     * Hilfsmethode: Finde letzten Timestamp, bei dem present=true war.
     */
    private Long getLastPresenceTimestamp(String positionPath) {
        Map<Long, PresenceEntry> all = getValues(0, Instant.now().toEpochMilli());
        return all.entrySet().stream()
                .filter(e -> e.getValue().positionPath().equals(positionPath))
                .filter(e -> e.getValue().present())
                .map(Map.Entry::getKey)
                .max(Long::compareTo)
                .orElse(null);
    }

    /**
     * Eintrag: Raum + Präsenzstatus
     */
    public record PresenceEntry(String positionPath, boolean present) {

        @Override
        public String toString() {
            return positionPath + "=" + (present ? "1" : "0");
        }

        public static PresenceEntry fromString(String s) {
            String[] parts = s.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Ungültiger Eintrag: " + s);
            }
            return new PresenceEntry(parts[0], "1".equals(parts[1]));
        }

        public static PresenceEntry isPresent(PositionPath positionPath) {
            return new PresenceEntry(positionPath.toString(), true);
        }
    }
}
