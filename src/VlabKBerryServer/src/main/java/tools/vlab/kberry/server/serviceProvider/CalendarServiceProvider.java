package tools.vlab.kberry.server.serviceProvider;

import io.vertx.core.Future;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.KNXDevice;

import java.util.List;

public interface CalendarServiceProvider {

    <T extends KNXDevice> Future<EventTime> getToday(PositionPath positionPath, Class<T> clazz);

    Future<List<Entry>> getToday();

    Future<List<Entry>> getTomorrow();

    record Entry(EventTime eventTime, String title, String description){}

}
