package tools.vlab.kberry.server.logic;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.actor.OnOffDevice;
import tools.vlab.kberry.core.devices.actor.Plug;
import tools.vlab.kberry.core.devices.sensor.PresenceSensor;
import tools.vlab.kberry.core.devices.sensor.PresenceStatus;

public class AutoPlugOnLogic extends Logic implements PresenceStatus {

    private AutoPlugOnLogic(PositionPath path) {
        super(path);
    }

    public static AutoPlugOnLogic at(PositionPath positionPath) {
        return new AutoPlugOnLogic(positionPath);
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
        if (!isSameRoom(sensor)) return;
        if (available) {
            this.getKnxDevices().getKNXDevice(Plug.class, sensor.getPositionPath()).ifPresent(OnOffDevice::on);
        }
    }
}
