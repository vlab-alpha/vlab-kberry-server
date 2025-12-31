package tools.vlab.kberry.server.serviceProvider;

import java.sql.Time;
import java.util.Optional;

public interface WeatherServiceProvider {

    Optional<Double> getTemperatureToday(String period);

    Optional<Double> getTemperaturTomorrow(String period);

    Optional<Time> getTodaySunset();

    Optional<Time> getTodaySunrise();

    Optional<Boolean> isDayLight(int hour);
}
