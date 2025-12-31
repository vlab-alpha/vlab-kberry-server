package tools.vlab.kberry.server.statistics;

import io.vertx.core.AbstractVerticle;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.core.devices.sensor.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StatisticsScheduler extends AbstractVerticle {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Statistics statistics;
    private final KNXDevices devices;

    public StatisticsScheduler(Statistics statistics, KNXDevices devices) {
        this.statistics = statistics;
        this.devices = devices;
    }

    @Override
    public void start() {
        executor.scheduleAtFixedRate(() -> {
            devices.getKNXDevices(TemperatureSensor.class).forEach(device -> statistics.getTemperatur().append(new TemperaturStatistics.TemperatureEntry(device.getPositionPath().getPath(), device.getCurrentTemp())));
            devices.getKNXDevices(VOCSensor.class).forEach(device -> statistics.getVoc().append(new VOCStatistics.VOCEntry(device.getPositionPath().getPath(), device.getCurrentPPM())));
            devices.getKNXDevices(PresenceSensor.class).forEach(device -> statistics.getPresent().append(new PresentStatistics.PresenceEntry(device.getPositionPath().getPath(), device.isPresent())));
            devices.getKNXDevices(HumiditySensor.class).forEach(device -> statistics.getHumidity().append(new HumidityStatistics.HumidityEntry(device.getPositionPath().getPath(), device.getCurrentHumidity())));
            devices.getKNXDevices(ElectricitySensor.class).forEach(device -> statistics.getElectricity().append(new ElectricityStatistics.ElectricityEntry(device.getPositionPath().getPath(), device.getCurrentKWHMeter())));
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        executor.shutdown();
    }
}
