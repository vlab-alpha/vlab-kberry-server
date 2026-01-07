package tools.vlab.kberry.server.serviceProvider;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MeteoWeatherVerticle extends AbstractVerticle implements WeatherServiceProvider {

    private static final Logger Log = LoggerFactory.getLogger(MeteoWeatherVerticle.class);

    private WebClient client;
    private double latitude;
    private double longitude;

    @Getter
    private final Map<String, Double> dailyTemperatures = new HashMap<>();
    @Getter
    private final Map<String, Double> nextDayTemperatures = new HashMap<>();
    @Getter
    private double currentTemperature = Double.NaN;

    @Getter
    private final Map<String, Integer> dailyWeatherCodes = new HashMap<>();
    @Getter
    private final Map<String, Integer> nextDayWeatherCodes = new HashMap<>();
    @Getter
    private String currentWeather = "unknown";

    @Getter
    private String sunriseToday = "";
    @Getter
    private String sunsetToday = "";
    @Getter
    private String sunriseTomorrow = "";
    @Getter
    private String sunsetTomorrow = "";

    private String configFile = "config/current-location";

    public MeteoWeatherVerticle() {
    }

    public MeteoWeatherVerticle(String configFile) {
        this.configFile = configFile;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        Log.info("Starting MeteoWeatherVerticle");
        client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost("api.open-meteo.com")
                .setDefaultPort(443)
                .setSsl(true)
        );
        readLocationFromFile()
                .compose(none -> fetchWeatherData())
                .compose(none -> {
                    getVertx().setPeriodic(6 * 60 * 60 * 1000L, id -> fetchWeatherData());
                    return Future.succeededFuture();
                })
                .onSuccess(none -> startPromise.complete())
                .onFailure(startPromise::fail);
    }

    private Future<Void> readLocationFromFile() {
        return this.getVertx().fileSystem().readFile(this.configFile)
                .compose(file -> {
                    var json = file.toJsonObject();
                    latitude = json.getDouble("latitude");
                    longitude = json.getDouble("longitude");
                    return Future.succeededFuture();
                });
    }

    private Future<Void> fetchWeatherData() {

        String url = "/v1/forecast?latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,weathercode&daily=sunrise,sunset&forecast_days=2&timezone=Europe/Berlin";

        return client.getAbs("https://api.open-meteo.com" + url)
                .send()
                .compose(response -> {
                    try {
                        JsonObject json = response.bodyAsJsonObject();

                        JsonObject hourly = json.getJsonObject("hourly");
                        if (hourly != null) {
                            JsonArray temps = hourly.getJsonArray("temperature_2m");
                            JsonArray times = hourly.getJsonArray("time");
                            JsonArray weatherCodes = hourly.getJsonArray("weathercode");

                            dailyTemperatures.clear();
                            nextDayTemperatures.clear();
                            dailyWeatherCodes.clear();
                            nextDayWeatherCodes.clear();

                            LocalDate todayDate = LocalDate.now();
                            LocalDate tomorrowDate = todayDate.plusDays(1);

                            for (int i = 0; i < times.size(); i++) {
                                String timeStr = times.getString(i);
                                double temp = temps.getDouble(i);
                                int code = weatherCodes.getInteger(i);

                                LocalDateTime ldt = LocalDateTime.parse(timeStr);
                                LocalDate datePart = ldt.toLocalDate();

                                if (datePart.equals(todayDate)) {
                                    dailyTemperatures.put(timeStr, temp);
                                    dailyWeatherCodes.put(timeStr, code);
                                } else if (datePart.equals(tomorrowDate)) {
                                    nextDayTemperatures.put(timeStr, temp);
                                    nextDayWeatherCodes.put(timeStr, code);
                                }
                            }

                            currentTemperature = findCurrentTemperature(times, temps);
                            Log.debug("Current temperature: {}", currentTemperature);
                            currentWeather = findCurrentWeather(times, weatherCodes);
                        }

                        // Sunrise / Sunset
                        JsonObject daily = json.getJsonObject("daily");
                        if (daily != null) {
                            JsonArray sunriseArr = daily.getJsonArray("sunrise");
                            JsonArray sunsetArr = daily.getJsonArray("sunset");

                            sunriseToday = sunriseArr.getString(0);
                            sunsetToday = sunsetArr.getString(0);
                            sunriseTomorrow = sunriseArr.getString(1);
                            sunsetTomorrow = sunsetArr.getString(1);
                        }
                        return Future.succeededFuture();
                    } catch (Exception e) {
                        Log.error("fetch weather data failed!", e);
                        return Future.failedFuture(e);
                    }
                });
    }

    private String findCurrentWeather(JsonArray times, JsonArray codes) {

        LocalDateTime now = LocalDateTime.now();
        long nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        int closestCode = codes.getInteger(0);
        long minDiff = Long.MAX_VALUE;

        for (int i = 0; i < times.size(); i++) {
            LocalDateTime ldt = LocalDateTime.parse(times.getString(i));
            long ts = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            long diff = Math.abs(nowMillis - ts);
            if (diff < minDiff) {
                minDiff = diff;
                closestCode = codes.getInteger(i);
            }
        }

        return mapWeatherCode(closestCode);
    }

    /**
     * Ermittelt die Temperatur, die zeitlich am nächsten zum aktuellen Zeitpunkt liegt.
     */
    private double findCurrentTemperature(JsonArray times, JsonArray temps) {
        LocalDateTime now = LocalDateTime.now();
        long nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        double closestTemp = temps.getDouble(0);
        long minDiff = Long.MAX_VALUE;

        for (int i = 0; i < times.size(); i++) {
            LocalDateTime ldt = LocalDateTime.parse(times.getString(i));
            long ts = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            long diff = Math.abs(nowMillis - ts);
            if (diff < minDiff) {
                minDiff = diff;
                closestTemp = temps.getDouble(i);
            }
        }
        return closestTemp;
    }

    // ------------------------ PUBLIC API ----------------------------

    @Override
    public Optional<Double> getTemperatureToday(String period) {
        return getTemperature(period, false);
    }

    @Override
    public Optional<Double> getTemperaturTomorrow(String period) {
        return getTemperature(period, true);
    }

    @Override
    public Optional<Boolean> isDayLight(int hour) {
        try {
            Optional<Time> sunriseOpt = getTodaySunrise();
            Optional<Time> sunsetOpt = getTodaySunset();

            if (sunriseOpt.isEmpty() || sunsetOpt.isEmpty()) return Optional.empty();

            int sunriseHour = sunriseOpt.get().toLocalTime().getHour();
            int sunsetHour = sunsetOpt.get().toLocalTime().getHour();

            return Optional.of(hour >= sunriseHour && hour < sunsetHour);

        } catch (Exception e) {
            Log.error("Is DayLight failed!", e);
            return Optional.empty();
        }
    }

    private Optional<Time> parseTime(String value) {
        if (value == null || value.isBlank()) return Optional.empty();

        try {
            LocalDateTime ldt = LocalDateTime.parse(value);
            return Optional.of(Time.valueOf(ldt.toLocalTime()));
        } catch (Exception e) {
            Log.error("Parse time failed!", e);
            return Optional.empty();
        }
    }

    public Optional<Time> getTodaySunrise() {
        return parseTime(sunriseToday);
    }

    public Optional<Time> getTodaySunset() {
        return parseTime(sunsetToday);
    }

    private Optional<Integer> getWeather(String period, boolean nextDay) {
        Map<String, Integer> map = nextDay ? nextDayWeatherCodes : dailyWeatherCodes;
        return switch (period.toLowerCase()) {
            case "morning" -> Optional.of(averageWeatherCode(map, 6, 11));
            case "afternoon" -> Optional.of(averageWeatherCode(map, 12, 17));
            case "evening" -> Optional.of(averageWeatherCode(map, 18, 23));
            case "night" -> Optional.of(averageWeatherCode(map, 0, 5));
            default -> Optional.empty();
        };
    }

    private int averageWeatherCode(Map<String, Integer> map, int startHour, int endHour) {
        Map<Integer, Integer> frequencies = new HashMap<>();

        for (String key : map.keySet()) {
            LocalDateTime ldt = LocalDateTime.parse(key);
            int hour = ldt.getHour();

            if (hour >= startHour && hour <= endHour) {
                int code = map.get(key);
                frequencies.put(code, frequencies.getOrDefault(code, 0) + 1);
            }
        }

        return frequencies.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);
    }

    // Temperaturbereiche
    private Optional<Double> getTemperature(String period, boolean nextDay) {
        Map<String, Double> map = nextDay ? nextDayTemperatures : dailyTemperatures;

        return switch (period.toLowerCase()) {
            case "morning" -> Optional.of(average(map, 6, 11));
            case "afternoon" -> Optional.of(average(map, 12, 17));
            case "evening" -> Optional.of(average(map, 18, 23));
            case "night" -> Optional.of(average(map, 0, 5));
            default -> Optional.empty();
        };
    }

    private double average(Map<String, Double> map, int startHour, int endHour) {
        double sum = 0;
        int count = 0;

        for (String key : map.keySet()) {
            LocalDateTime ldt = LocalDateTime.parse(key);
            int hour = ldt.getHour();

            if (hour >= startHour && hour <= endHour) {
                sum += map.get(key);
                count++;
            }
        }
        return count > 0 ? sum / count : Double.NaN;
    }

    private String mapWeatherCode(int code) {
        return switch (code) {
            case 0 -> "sunny";
            case 1, 2 -> "partly_cloudy";
            case 3 -> "cloudy";

            case 45, 48 -> "foggy";

            case 51, 53, 55 -> "drizzle";
            case 56, 57 -> "freezing_drizzle";

            case 61, 63, 65 -> "rain";
            case 66, 67 -> "freezing_rain";

            case 71, 73, 75 -> "snow";
            case 77 -> "snow_grains";

            case 80, 81, 82 -> "rain_showers";
            case 85, 86 -> "snow_showers";

            case 95 -> "thunderstorm";
            case 96, 99 -> "thunderstorm_with_hail";

            default -> "unknown";
        };
    }

}