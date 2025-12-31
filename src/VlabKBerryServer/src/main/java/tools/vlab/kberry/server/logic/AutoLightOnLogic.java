package tools.vlab.kberry.server.logic;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.actor.Light;
import tools.vlab.kberry.core.devices.actor.OnOffDevice;
import tools.vlab.kberry.core.devices.sensor.LuxSensor;
import tools.vlab.kberry.core.devices.sensor.LuxStatus;
import tools.vlab.kberry.core.devices.sensor.PresenceSensor;
import tools.vlab.kberry.core.devices.sensor.PresenceStatus;

import java.util.List;
import java.util.Vector;

public class AutoLightOnLogic extends Logic implements PresenceStatus, LuxStatus {

    private final float minLux;

    private AutoLightOnLogic(float minLux, Vector<PositionPath> paths) {
        super(paths);
        this.minLux = minLux;
    }

    public static AutoLightOnLogic at(float minLux, PositionPath... positionPath) {
        return new AutoLightOnLogic(minLux, new Vector<>(List.of(positionPath)));
    }

    public static AutoLightOnLogic at(PositionPath... positionPath) {
        return new AutoLightOnLogic(0, new Vector<>(List.of(positionPath)));
    }

    @Override
    public void stop() {
        // Ignore
    }

    @Override
    public void start() {
        // Ignore
    }

    @Override
    public void presenceChanged(PresenceSensor sensor, boolean available) {
        if (available) {
            switchOnLightByLux(sensor.getPositionPath());
        }
    }

    @Override
    public void luxChanged(LuxSensor sensor, float lux) {
        var presence = this.getKnxDevices().getKNXDevice(PresenceSensor.class, sensor.getPositionPath());
        if (presence.isPresent() && presence.get().isPresent()) {
            switchOnLightByLux(sensor.getPositionPath());
        }
    }

    // Problem only any light can be switch on, so if the room has many lights and you need specific light to switch on
    private void switchOnLightByLux(PositionPath positionPath) {
        var light = this.getKnxDevices().getKNXDeviceByRoom(Light.class, positionPath);
        var luxSensor = this.getKnxDevices().getKNXDeviceByRoom(LuxSensor.class, positionPath);
        if (light.isPresent() && luxSensor.isPresent()) {
            if (luxSensor.get().getCurrentLux() < minLux) {
                light.get().on();
            }
        } else light.ifPresent(OnOffDevice::on);
    }
}
