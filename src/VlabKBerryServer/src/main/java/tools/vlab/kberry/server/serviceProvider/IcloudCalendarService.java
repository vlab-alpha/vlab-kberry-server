package tools.vlab.kberry.server.serviceProvider;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.List;

public class IcloudCalendarService extends AbstractVerticle implements CalendarServiceProvider {

    private static final Logger Log = LoggerFactory.getLogger(IcloudCalendarService.class);

    private final String username;
    private final String appPassword;
    private final String calendarUrl; // iCloud CalDAV URL

    private IcloudCalendarService(String username, String appPassword, String calendarUrl) {
        this.username = username;
        this.appPassword = appPassword;
        this.calendarUrl = calendarUrl;
    }

    public static IcloudCalendarService ical(String username, String appPassword, String calendarUrl) {
        return new IcloudCalendarService(username, appPassword, calendarUrl);
    }

    private Future<Boolean> hasEvents(LocalDate date) {
        return getVertx().executeBlocking(() -> {
            Sardine sardine = SardineFactory.begin(username, appPassword);
            List<DavResource> resources = sardine.list(calendarUrl);

            Instant dayStart = date.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant dayEnd = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

            for (DavResource res : resources) {
                if (!res.isDirectory() && res.getName().endsWith(".ics")) {
                    try (InputStream is = sardine.get(res.getHref().toString())) {
                        CalendarBuilder builder = new CalendarBuilder();
                        Calendar calendar = builder.build(is);

                        for (Component component : calendar.getComponents(Component.VEVENT)) {
                            VEvent event = (VEvent) component;
                            Instant start = toInstant(event.getDateTimeStart().getDate());
                            Instant end = toInstant(event.getDateTimeEnd().getDate());

                            if ((start.isBefore(dayEnd) && end.isAfter(dayStart))) {
                                return true; // Busy
                            }
                        }
                    } catch (Exception e) {
                        Log.error("Error reading calendar resource", e);
                    }
                }
            }
            return false; // Keine Events gefunden
        });
    }

    private static Instant toInstant(Temporal temporal) {
        return switch (temporal) {
            case ZonedDateTime zdtStart -> zdtStart.toInstant();
            case LocalDateTime ldtStart -> ldtStart.atZone(ZoneId.systemDefault()).toInstant();
            case LocalDate ldStart -> ldStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
            default -> throw new IllegalArgumentException("Unsupported Temporal type: " + temporal.getClass());
        };
    }


    @Override
    public Future<Boolean> isAvailableNow() {
        return hasEvents(LocalDate.now());
    }

    @Override
    public Future<Boolean> tomorrowAvailable() {
        return hasEvents(LocalDate.now().plusDays(1));
    }

    @Override
    public Future<Boolean> availableInTwoDays() {
        return hasEvents(LocalDate.now().plusDays(2));
    }
}