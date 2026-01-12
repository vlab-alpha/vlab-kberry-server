package tools.vlab.kberry.server.statistics;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import tools.vlab.kberry.core.PositionPath;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class PresentStatistics
        extends Statistic<PresentStatistics.PresenceEntry> {

    public PresentStatistics(Vertx vertx) {
        super(vertx, "stat/presence.csv");
    }

    @Override
    protected String serializeValue(PresenceEntry value) {
        return value.toString();
    }

    @Override
    protected PresenceEntry deserializeValue(String line) {
        return PresenceEntry.fromString(line);
    }

    public Future<Double> calculateUsage(long from, long to, PositionPath positionPath) {
        return getValues(from, to)
                .map(values -> values.values().stream()
                        .filter(e -> e.positionPath().toLowerCase().startsWith(positionPath.getPath().toLowerCase()))
                        .mapToDouble(e -> e.present() ? 1.0 : 0.0)
                        .average()
                        .orElse(0.0) * 100);
    }

    public Future<Double> getCurrentAverageUsage(PositionPath positionPath) {
        return calculateUsage(0, Instant.now().toEpochMilli(), positionPath);
    }

    public Future<Double> getUsageLastDay(PositionPath positionPath) {
        return calculateUsage(
                Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(),
                Instant.now().toEpochMilli(),
                positionPath
        );
    }

    public Future<Double> getUsageLastMonth(PositionPath positionPath) {
        return calculateUsage(
                Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli(),
                Instant.now().toEpochMilli(),
                positionPath
        );
    }

    public Future<Double> getUsageLastYear(PositionPath positionPath) {
        return calculateUsage(
                Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli(),
                Instant.now().toEpochMilli(),
                positionPath
        );
    }

    private Future<Long> getLastPresenceTimestamp(PositionPath positionPath) {
        return getValues(0, Instant.now().toEpochMilli())
                .map(values -> values.entrySet().stream()
                        .filter(e -> e.getValue().positionPath().toLowerCase().startsWith(positionPath.getPath().toLowerCase()))
                        .filter(e -> e.getValue().present())
                        .mapToLong(Map.Entry::getKey)
                        .max()
                        .orElse(-1L));
    }

    public Future<Long> getLastPresenceMinutes(PositionPath positionPath) {
        return getLastPresenceTimestamp(positionPath)
                .map(ts -> ts == -1 ? -1 : ChronoUnit.MINUTES.between(Instant.ofEpochMilli(ts), Instant.now()));
    }

    public Future<Long> getLastPresenceHours(PositionPath positionPath) {
        return getLastPresenceTimestamp(positionPath)
                .map(ts -> ts == -1 ? -1 : ChronoUnit.HOURS.between(Instant.ofEpochMilli(ts), Instant.now()));
    }

    public Future<Long> getLastPresenceDays(PositionPath positionPath) {
        return getLastPresenceTimestamp(positionPath)
                .map(ts -> ts == -1 ? -1 : ChronoUnit.DAYS.between(Instant.ofEpochMilli(ts), Instant.now()));
    }

    public record PresenceEntry(String positionPath, boolean present) {

        @Override
        public String toString() {
            return positionPath + "=" + (present ? "1" : "0");
        }

        public static PresenceEntry fromString(String s) {
            String[] parts = s.split("=", 2);
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