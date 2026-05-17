package tools.vlab.kberry.server.logic;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.sensor.PresenceSensor;
import tools.vlab.kberry.core.knx.devices.sensor.PresenceStatus;
import tools.vlab.kberry.core.mqtt.shelly.devices.device.Plug;

public class AutoShellyPlugOnLogic extends Logic implements PresenceStatus {

    public final static String LOGIC_NAME = "AutoPlugOn";

    private AutoShellyPlugOnLogic(PositionPath path) {
        super(LOGIC_NAME, path);
    }

    public static AutoShellyPlugOnLogic at(PositionPath positionPath) {
        return new AutoShellyPlugOnLogic(positionPath);
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
        if (isNotSameRoom(sensor)) return;
        if (available) {
            this.getShellyDevices().getDevice(Plug.class, sensor.getPositionPath()).ifPresent(Plug::on);
        }
    }
}
