package tools.vlab.kberry.server.statistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import tools.vlab.kberry.core.knx.devices.KNXDevices;
import tools.vlab.kberry.core.knx.devices.sensor.*;
import tools.vlab.kberry.server.log.Logger;
import tools.vlab.kberry.server.statistics.values.BooleanValue;
import tools.vlab.kberry.server.statistics.values.FloatValue;

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
        this.vertx.deployVerticle(this.statistics.getElectricity())
                .compose(done -> this.vertx.deployVerticle(this.statistics.getVoc()))
                .compose(done -> this.vertx.deployVerticle(this.statistics.getHumidity()))
                .compose(done -> this.vertx.deployVerticle(this.statistics.getTemperatur()))
                .onSuccess(none -> startPromise.complete())
                .onFailure(startPromise::fail);
        timerId = this.getVertx().setPeriodic(60 * 1000, (id) -> {
            try {
                devices.getKNXDevices(TemperatureSensor.class).forEach(device -> statistics.getTemperatur().append(device.getPositionPath(), FloatValue.of(device.getCurrentTemp())));
                devices.getKNXDevices(VOCSensor.class).forEach(device -> statistics.getVoc().append(device.getPositionPath(), FloatValue.of(device.getCurrentPPM())));
                devices.getKNXDevices(PresenceSensor.class).forEach(device -> statistics.getPresent().append(device.getPositionPath(), BooleanValue.of(device.isPresent())));
                devices.getKNXDevices(HumiditySensor.class).forEach(device -> statistics.getHumidity().append(device.getPositionPath(), FloatValue.of(device.getCurrentHumidity())));
                devices.getKNXDevices(ElectricitySensor.class).forEach(device -> statistics.getElectricity().append(device.getPositionPath(), FloatValue.of(device.getCurrentKWHMeter())));
            } catch (Exception e) {
                Logger.error("Error in StatisticsScheduler", e);
            }
        });
    }

    @Override
    public void stop() {
        if (timerId != null) {
            this.getVertx().cancelTimer(timerId);
        }
    }
}
