package tools.vlab.kberry.server.serviceProvider;

import io.vertx.core.Future;

public interface CalendarServiceProvider {

    Future<Boolean> tomorrowAvailable();
    Future<Boolean> isAvailableNow();
    Future<Boolean> availableInTwoDays();
}
