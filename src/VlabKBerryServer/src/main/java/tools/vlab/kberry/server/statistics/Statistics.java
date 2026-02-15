package tools.vlab.kberry.server.statistics;

import lombok.Data;
import tools.vlab.kberry.server.statistics.values.BooleanValue;
import tools.vlab.kberry.server.statistics.values.FloatValue;

@Data
public class Statistics {
    private final Statistic<FloatValue> electricity;
    private final Statistic<BooleanValue> present;
    private final Statistic<FloatValue> temperatur;
    private final Statistic<FloatValue> voc;
    private final Statistic<FloatValue> humidity;

    public Statistics(String folder) {
        electricity = new Statistic<>(folder);
        present = new Statistic<>(folder);
        temperatur = new Statistic<>(folder);
        voc = new Statistic<>(folder);
        humidity = new Statistic<>(folder);
    }

}
