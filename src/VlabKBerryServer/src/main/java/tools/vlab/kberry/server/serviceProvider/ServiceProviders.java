package tools.vlab.kberry.server.serviceProvider;

import java.util.Map;

public record ServiceProviders(CostWattServiceProvider costWattServiceProvider,
                               WeatherServiceProvider temperaturServiceProvider,
                               Map<String, IcsCalendarServiceProvider> calendarServiceProvider) {

}
