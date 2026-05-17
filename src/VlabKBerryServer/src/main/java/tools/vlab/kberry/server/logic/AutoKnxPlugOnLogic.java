package tools.vlab.kberry.server.logic;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.actor.OnOffDevice;
import tools.vlab.kberry.core.knx.devices.actor.Plug;
import tools.vlab.kberry.core.knx.devices.sensor.PresenceSensor;
import tools.vlab.kberry.core.knx.devices.sensor.PresenceStatus;

public class AutoKnxPlugOnLogic extends Logic implements PresenceStatus {

    public final static String LOGIC_NAME = "AutoKnxPlugOn";

    private AutoKnxPlugOnLogic(PositionPath path) {
        super(LOGIC_NAME, path);
    }

    public static AutoKnxPlugOnLogic at(PositionPath positionPath) {
        return new AutoKnxPlugOnLogic(positionPath);
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
            this.getKnxDevices().getKNXDevice(Plug.class, sensor.getPositionPath()).ifPresent(OnOffDevice::on);
        }
    }
}
