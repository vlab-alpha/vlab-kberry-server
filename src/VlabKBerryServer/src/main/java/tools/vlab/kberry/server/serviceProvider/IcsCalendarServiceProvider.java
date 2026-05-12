package tools.vlab.kberry.server.serviceProvider;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.KNXDevice;

import java.io.InputStream;
import java.net.URL;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.List;

public class IcsCalendarServiceProvider implements CalendarServiceProvider {

    private final Vertx vertx;
    private final String url;
    private final InputStream testStream;

    public IcsCalendarServiceProvider(Vertx vertx, String url) {
        this(vertx, url, null);
    }

    public IcsCalendarServiceProvider(Vertx vertx, InputStream testStream) {
        this(vertx, null, testStream);
    }

    private IcsCalendarServiceProvider(Vertx vertx, String url, InputStream testStream) {
        this.vertx = vertx;
        this.url = url != null ? normalize(url) : null;
        this.testStream = testStream;
    }

    @Override
    public <T extends KNXDevice> Future<EventTime> getToday(PositionPath positionPath, Class<T> clazz) {
        return vertx.executeBlocking(() -> {
            try (InputStream is = (testStream != null) ? testStream : new URL(url).openStream()) {
                CalendarBuilder builder = new CalendarBuilder();
                var calendar = builder.build(is);

                LocalDate today = LocalDate.now();
                ZoneId zone = ZoneId.systemDefault();
                Instant dayStart = today.atStartOfDay(zone).toInstant();
                Instant dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();
                String name = positionPath.getId();

                for (var component : calendar.getComponents(Component.VEVENT)) {
                    VEvent event = (VEvent) component;
                    var t = event.getSummary().getValue();
                    if (event.getSummary() == null || !event.getSummary().getValue().contains(name)) {
                        continue;
                    }

                    var positionPathType = event.getSummary().getValue().split(":");
                    if (positionPathType.length != 2) {
                        continue;
                    }
                    var type = positionPathType[0];

                    if (!clazz.getSimpleName().equalsIgnoreCase(type)) {
                        continue;
                    }



                    // Properties sicher holen
                    Temporal startTemp = event.getProperty(Property.DTSTART)
                            .map(p -> ((DtStart<?>) p).getDate())
                            .orElse(null);

                    Temporal endTemp = event.getProperty(Property.DTEND)
                            .map(p -> ((DtEnd<?>) p).getDate())
                            .orElse(null);

                    if (startTemp == null || endTemp == null) continue;

                    // Konvertierung von Temporal zu Instant
                    Instant start = toInstant(startTemp, zone);
                    Instant end = toInstant(endTemp, zone);

                    if (start.isBefore(dayEnd) && end.isAfter(dayStart)) {
                        return new EventTime(
                                LocalDateTime.ofInstant(start, zone),
                                LocalDateTime.ofInstant(end, zone)
                        );
                    }
                }
                return null;

            } catch (Exception e) {
                throw new RuntimeException("Fehler beim ICS-Parsing", e);
            }
        });
    }

    @Override
    public Future<List<Entry>> getToday() {
        return vertx.executeBlocking(() -> {
            try (InputStream is = (testStream != null) ? testStream : new URL(url).openStream()) {

                CalendarBuilder builder = new CalendarBuilder();
                var calendar = builder.build(is);

                LocalDate today = LocalDate.now();
                ZoneId zone = ZoneId.systemDefault();
                Instant dayStart = today.atStartOfDay(zone).toInstant();
                Instant dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();

                List<Entry> result = new java.util.ArrayList<>();

                for (var component : calendar.getComponents(Component.VEVENT)) {

                    VEvent event = (VEvent) component;

                    // SUMMARY
                    if (event.getSummary() == null) continue;
                    String title = event.getSummary().getValue();

                    // DESCRIPTION optional
                    String description = null;
                    var descProp = event.getDescription();
                    if (descProp != null) {
                        description = descProp.getValue();
                    }

                    // DTSTART / DTEND holen
                    Temporal startTemp = event.getProperty(Property.DTSTART)
                            .map(p -> ((DtStart<?>) p).getDate())
                            .orElse(null);

                    Temporal endTemp = event.getProperty(Property.DTEND)
                            .map(p -> ((DtEnd<?>) p).getDate())
                            .orElse(null);

                    if (startTemp == null || endTemp == null) continue;

                    Instant start = toInstant(startTemp, zone);
                    Instant end = toInstant(endTemp, zone);

                    // Nur Events, die heute stattfinden
                    if (start.isBefore(dayEnd) && end.isAfter(dayStart)) {

                        EventTime eventTime = new EventTime(
                                LocalDateTime.ofInstant(start, zone),
                                LocalDateTime.ofInstant(end, zone)
                        );

                        result.add(new Entry(eventTime, title, description));
                    }
                }

                return result;

            } catch (Exception e) {
                throw new RuntimeException("Fehler beim ICS-Parsing", e);
            }
        });
    }

    @Override
    public Future<List<Entry>> getTomorrow() {
        return vertx.executeBlocking(() -> {
            try (InputStream is = (testStream != null) ? testStream : new URL(url).openStream()) {

                CalendarBuilder builder = new CalendarBuilder();
                var calendar = builder.build(is);

                LocalDate tomorrow = LocalDate.now().plusDays(1);
                ZoneId zone = ZoneId.systemDefault();
                Instant dayStart = tomorrow.atStartOfDay(zone).toInstant();
                Instant dayEnd = tomorrow.plusDays(1).atStartOfDay(zone).toInstant();

                List<Entry> result = new java.util.ArrayList<>();

                for (var component : calendar.getComponents(Component.VEVENT)) {

                    VEvent event = (VEvent) component;

                    // SUMMARY
                    if (event.getSummary() == null) continue;
                    String title = event.getSummary().getValue();

                    // DESCRIPTION optional
                    String description = null;
                    var descProp = event.getDescription();
                    if (descProp != null) {
                        description = descProp.getValue();
                    }

                    // DTSTART / DTEND holen
                    Temporal startTemp = event.getProperty(Property.DTSTART)
                            .map(p -> ((DtStart<?>) p).getDate())
                            .orElse(null);

                    Temporal endTemp = event.getProperty(Property.DTEND)
                            .map(p -> ((DtEnd<?>) p).getDate())
                            .orElse(null);

                    if (startTemp == null || endTemp == null) continue;

                    Instant start = toInstant(startTemp, zone);
                    Instant end = toInstant(endTemp, zone);

                    // Nur Events, die morgen stattfinden
                    if (start.isBefore(dayEnd) && end.isAfter(dayStart)) {

                        EventTime eventTime = new EventTime(
                                LocalDateTime.ofInstant(start, zone),
                                LocalDateTime.ofInstant(end, zone)
                        );

                        result.add(new Entry(eventTime, title, description));
                    }
                }

                return result;

            } catch (Exception e) {
                throw new RuntimeException("Fehler beim ICS-Parsing", e);
            }
        });
    }

    /**
     * Wandelt die verschiedenen Temporal-Typen von ical4j (LocalDate, LocalDateTime, ZonedDateTime)
     * sicher in ein Instant um.
     */
    private Instant toInstant(Temporal temporal, ZoneId zone) {
        if (temporal instanceof Instant) {
            return (Instant) temporal;
        } else if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toInstant();
        } else if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).toInstant();
        } else if (temporal instanceof LocalDateTime) {
            return ((LocalDateTime) temporal).atZone(zone).toInstant();
        } else if (temporal instanceof LocalDate) {
            return ((LocalDate) temporal).atStartOfDay(zone).toInstant();
        } else {
            return Instant.from(temporal);
        }
    }

    private String normalize(String url) {
        if (url != null && url.startsWith("webcal://")) {
            return "https://" + url.substring(9);
        }
        return url;
    }
}