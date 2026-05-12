package tools.vlab.kberry.server.logic;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.actor.Light;
import tools.vlab.kberry.core.knx.devices.sensor.LuxSensor;
import tools.vlab.kberry.core.knx.devices.sensor.LuxStatus;
import tools.vlab.kberry.core.knx.devices.sensor.PresenceSensor;
import tools.vlab.kberry.core.knx.devices.sensor.PresenceStatus;
import tools.vlab.kberry.server.log.Logger;


// FIXME: Wenn in den Raum zwei Leuchten drinnen sind, dann kann es sein, dass manchmal das Licht sich nicht einschaltet, bei dem die Logic zugeordnet ist.
public class AutoLightOnLogic extends Logic implements PresenceStatus, LuxStatus {

    public final static String LOGIC_NAME = "autoLightOn";
    private final static long IGNORE_S = 3;
    private final float minLux;

    private AutoLightOnLogic(float minLux, PositionPath path) {
        super(LOGIC_NAME, path);
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
        if (isNotSameRoom(sensor)) {
            Logger.debug(sensor, "AUTO LIGHT ON: Ignore Input");
            return;
        }

        if (available) {
            Logger.debug(sensor, "AUTO LIGHT ON: Available ...");
            switchOnLightByLux();
        } else {
            Logger.debug(sensor, "AUTO LIGHT ON: Ignore because not available ...");
        }
    }

    @Override
    public void luxChanged(LuxSensor sensor, float lux) {
        if (isNotSameRoom(sensor)) return;

        if (minLux > 0) {
            var presence = this.getKnxDevices().getKNXDevice(PresenceSensor.class, sensor.getPositionPath());
            if (presence.isPresent() && presence.get().isPresent()) {
                Logger.debug(sensor,"AUTO LIGHT ON: Lux changed received!");
                switchOnLightByLux();
            }
        }
    }

    // Problem only any light can be switch on, so if the room has many lights and you need specific light to switch on
    private void switchOnLightByLux() {
        try {
            var light = this.getKnxDevices().getKNXDeviceByRoom(Light.class, this.getPositionPath());
            if (light.isPresent()) {
                if (light.get().getLastPresentSecond() > IGNORE_S) {
                    var luxSensor = this.getKnxDevices().getKNXDeviceByRoom(LuxSensor.class, getPositionPath());
                    if (minLux <= 0 || luxSensor.isPresent() && luxSensor.get().getCurrentLux() <= 0 || luxSensor.isEmpty() || luxSensor.get().getSmoothedLux() <= minLux) {
                        Logger.info(light.get(),"AUTO LIGHT ON: send switch on command ...");
                        light.ifPresent(Light::on);
                    } else {
                        Logger.debug(light.get(), "AUTO LIGHT ON: Lux is too less [min:{}; current:{}]", minLux, luxSensor.get().getCurrentLux());
                    }
                } else {
                    Logger.debug(light.get(), "AUTO LIGHT ON: Ignore Presence because switch off the light {}s ago (< {}s)", light.get().getLastPresentSecond(), IGNORE_S);
                }
            }
        } catch (Exception e) {
            Logger.error(getPositionPath(), "AUTO LIGHT ON: Switching on light Failed!", e);
        }
    }
}
