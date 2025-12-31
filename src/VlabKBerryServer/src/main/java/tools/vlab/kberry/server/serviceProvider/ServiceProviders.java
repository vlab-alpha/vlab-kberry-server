package tools.vlab.kberry.server.serviceProvider;

public record ServiceProviders(CostWattServiceProvider costWattServiceProvider, WeatherServiceProvider temperaturServiceProvider, CalendarServiceProvider calendarServiceProvider) {

}
