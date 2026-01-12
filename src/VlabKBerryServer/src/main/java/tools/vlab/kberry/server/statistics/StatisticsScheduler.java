package tools.vlab.kberry.server.statistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.core.devices.sensor.*;

public class StatisticsScheduler extends AbstractVerticle {

    private final Statistics statistics;
    private final KNXDevices devices;
    private Long timerId = null;

    public StatisticsScheduler(Statistics statistics, KNXDevices devices) {
        this.statistics = statistics;
        this.devices = devices;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        this.statistics.getTemperatur().start()
                .compose(none -> this.statistics.getVoc().start())
                .compose(none -> this.statistics.getPresent().start())
                .compose(none -> this.statistics.getHumidity().start())
                .compose(none -> this.statistics.getElectricity().start())
                .onSuccess(none -> startPromise.complete())
                .onFailure(startPromise::fail);
    }

    @Override
    public void start() {

        timerId = this.getVertx().setPeriodic(1000 * 60, (id) -> {
            devices.getKNXDevices(TemperatureSensor.class).forEach(device -> statistics.getTemperatur().append(new TemperaturStatistics.TemperatureEntry(device.getPositionPath().getPath(), device.getCurrentTemp())));
            devices.getKNXDevices(VOCSensor.class).forEach(device -> statistics.getVoc().append(new VOCStatistics.VOCEntry(device.getPositionPath().getPath(), device.getCurrentPPM())));
            devices.getKNXDevices(PresenceSensor.class).forEach(device -> statistics.getPresent().append(new PresentStatistics.PresenceEntry(device.getPositionPath().getPath(), device.isPresent())));
            devices.getKNXDevices(HumiditySensor.class).forEach(device -> statistics.getHumidity().append(new HumidityStatistics.HumidityEntry(device.getPositionPath().getPath(), device.getCurrentHumidity())));
            devices.getKNXDevices(ElectricitySensor.class).forEach(device -> statistics.getElectricity().append(new ElectricityStatistics.ElectricityEntry(device.getPositionPath().getPath(), device.getCurrentKWHMeter())));
        });
    }

    @Override
    public void stop() {
        if (timerId != null) {
            this.getVertx().cancelTimer(timerId);
        }
    }
}
