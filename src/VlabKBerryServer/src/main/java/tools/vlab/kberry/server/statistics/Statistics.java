package tools.vlab.kberry.server.statistics;

import lombok.Data;

@Data
public class Statistics {
    private final ElectricityStatistics electricity;
    private final PresentStatistics present;
    private final TemperaturStatistics temperatur;
    private final VOCStatistics voc;
    private final HumidityStatistics humidity;

    public Statistics() throws java.io.IOException {
        electricity = new ElectricityStatistics();
        present = new PresentStatistics();
        temperatur = new TemperaturStatistics();
        voc = new VOCStatistics();
        humidity = new HumidityStatistics();
    }

}
