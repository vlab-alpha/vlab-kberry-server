package tools.vlab.kberry.server.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.actor.Light;
import tools.vlab.kberry.core.devices.sensor.LuxSensor;
import tools.vlab.kberry.core.devices.sensor.LuxStatus;
import tools.vlab.kberry.core.devices.sensor.PresenceSensor;
import tools.vlab.kberry.core.devices.sensor.PresenceStatus;

public class AutoLightOnLogic extends Logic implements PresenceStatus, LuxStatus {

    private final static long IGNORE_S = 3;
    private static final Logger Log = LoggerFactory.getLogger(AutoLightOnLogic.class);
    private final float minLux;

    private AutoLightOnLogic(float minLux, PositionPath path) {
        super(path);
        this.minLux = minLux;
    }

    public static AutoLightOnLogic at(float minLux, PositionPath positionPath) {
        return new AutoLightOnLogic(minLux, positionPath);
    }

    public static AutoLightOnLogic at(PositionPath positionPath) {
        return new AutoLightOnLogic(0, positionPath);
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
        if (!isSameRoom(sensor)) {
            Log.debug("AUTO LIGHT: Not same room {}",sensor.getPositionPath());
            return;
        }

        if (available) {
            Log.debug("AUTO LIGHT Presence {}", sensor.getPositionPath());
            switchOnLightByLux();
        } else {
            Log.info("AUTO LIGHT Presence not available");
        }
    }

    @Override
    public void luxChanged(LuxSensor sensor, float lux) {
        if (!isSameRoom(sensor)) return;

        if (minLux > 0) {
            var presence = this.getKnxDevices().getKNXDevice(PresenceSensor.class, sensor.getPositionPath());
            if (presence.isPresent() && presence.get().isPresent()) {
                switchOnLightByLux();
            }
        }
    }

    // Problem only any light can be switch on, so if the room has many lights and you need specific light to switch on
    private void switchOnLightByLux() {
        try {
            Log.debug("Switching on light room {}", getPositionPath().getRoom());
            var light = this.getKnxDevices().getKNXDeviceByRoom(Light.class, this.getPositionPath());
            if (light.isPresent()) {
                if (light.get().getLastPresentSecond() > IGNORE_S) {
                    var luxSensor = this.getKnxDevices().getKNXDeviceByRoom(LuxSensor.class, getPositionPath());

                    luxSensor.ifPresentOrElse(
                            sensor -> Log.debug("LUX STATUS: S:{} C:{}", sensor.getSmoothedLux(), sensor.getSmoothedLux()),
                            () -> Log.debug("Kein LUX Sensor")
                    );

                    if (minLux <= 0 || luxSensor.isPresent() && luxSensor.get().getCurrentLux() <= 0 || luxSensor.isEmpty() || luxSensor.get().getSmoothedLux() <= minLux) {
                        light.ifPresent(Light::on);
                    }
                }
            }
        } catch (Exception e) {
            Log.error("Switching on light Failed for path {}", getPositionPath().getPath(), e);
        }
    }
}
