package tools.vlab.kberry.server.statistics;

import io.vertx.core.Vertx;
import lombok.Data;

@Data
public class Statistics {
    private final ElectricityStatistics electricity;
    private final PresentStatistics present;
    private final TemperaturStatistics temperatur;
    private final VOCStatistics voc;
    private final HumidityStatistics humidity;

    public Statistics(Vertx vertx) {
        electricity = new ElectricityStatistics(vertx);
        present = new PresentStatistics(vertx);
        temperatur = new TemperaturStatistics(vertx);
        voc = new VOCStatistics(vertx);
        humidity = new HumidityStatistics(vertx);
    }

}
