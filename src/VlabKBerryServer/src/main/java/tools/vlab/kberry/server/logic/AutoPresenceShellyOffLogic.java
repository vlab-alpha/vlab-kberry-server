package tools.vlab.kberry.server.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.sensor.PresenceSensor;
import tools.vlab.kberry.core.knx.devices.sensor.PresenceStatus;
import tools.vlab.kberry.core.mqtt.shelly.devices.device.Plug;
import tools.vlab.kberry.core.mqtt.shelly.devices.device.PlugStatus;
import tools.vlab.kberry.core.mqtt.shelly.devices.device.ShellySwitch;
import tools.vlab.kberry.server.log.Logger;

import java.util.concurrent.ConcurrentHashMap;

// FIXME: wenn es zwei Sensoren und zwei Leuchten gibt, dann schaltet es manchmal aus, obwohl in dem Raum (aber am anderen Sensor) trotzdem einer im Raum ist!!
public class AutoPresenceShellyOffLogic<T extends ShellySwitch> extends Logic implements PlugStatus, PresenceStatus {

    public final static String LOGIC_NAME = "AutoPresenceShellyOff";

    private final int followupTimeS;
    private final ConcurrentHashMap<String, OffTimer> presence = new ConcurrentHashMap<>();
    private Long timerId = null;
    private final Class<T> tClass;

    private AutoPresenceShellyOffLogic(int followupTimeS, PositionPath pathOfLight, Class<T> tClass) {
        super(LOGIC_NAME, pathOfLight);
        this.followupTimeS = followupTimeS;
        this.tClass = tClass;
    }

    public static <T extends ShellySwitch> AutoPresenceShellyOffLogic<T> at(int followupTimeS, PositionPath pathOfLight, Class<T> tclass) {
        return new AutoPresenceShellyOffLogic<T>(followupTimeS, pathOfLight, tclass);
    }


    @Override
    public void isOnChanged(Plug device, Boolean isOn) {
        if (isNotSamePosition(device)) {
            Logger.debug(device, "AUTO LIGHT OFF: SKIP Device does not have logic [Status:{}]", isOn);
            return;
        }

        if (isOn) {
            Logger.debug(device, "AUTO LIGHT OFF: Init Timer");
            presence.put(device.getPositionPath().getRoom(), OffTimer.init(device.getPositionPath(), followupTimeS));
        } else {
            Logger.debug(device,"AUTO LIGHT OFF: Remove Timer");
            presence.remove(device.getPositionPath().getRoom());
        }
    }

    @Override
    public void presenceChanged(PresenceSensor sensor, boolean available) {
        if (isNotSameRoom(sensor)) return;

        if (presence.containsKey(sensor.getPositionPath().getRoom())) {
            if (available) {
                Logger.debug(sensor, "AUTO DEVICE {} OFF: Someone in the room reset timer ...", tClass);
                presence.get(sensor.getPositionPath().getRoom()).reset();
            } else {
                Logger.debug(sensor, "AUTO DEVICE {} OFF: Nobody in the room start timer ...", tClass);
                presence.get(sensor.getPositionPath().getRoom()).start();
            }
        }
    }

    private void startPeriodic() {
        timerId = this.getVertx().setPeriodic(5000, v -> {
            for (String room : presence.keySet()) {
                var timer = presence.get(room);
                if (!timer.within()) {
                    try {
                        this.getShellyDevices().getDevicesByRoom(this.tClass, timer.getPositionPath()).forEach(device -> {
                            Logger.debug(device, "AUTO {} OFF: is not in the time range, switch off the light", tClass);
                            device.off();
                        });
                    } catch (Exception e) {
                        Logger.error(timer.getPositionPath(), e,"AUTO {} OFF: Check Periodic Presence failed!", tClass);
                    }
                }
            }
        });
    }

    private void checkCurrentLights() {
        this.getShellyDevices().getDevice(this.tClass, this.getPositionPath())
                .filter(ShellySwitch::isOn)
                .ifPresent(lightOn -> presence.put(
                        lightOn.getPositionPath().getRoom(),
                        OffTimer.init(lightOn.getPositionPath(), followupTimeS))
                );
    }

    @Override
    public void stop() {
        if (this.timerId != null) {
            this.getVertx().cancelTimer(this.timerId);
        }
    }

    @Override
    public void start() {
        startPeriodic();
        checkCurrentLights();
    }



    @AllArgsConstructor
    @Getter
    static class OffTimer {
        PositionPath positionPath;
        Long timer = null;
        long followupTimeMS;

        public static OffTimer init(PositionPath positionPath, long followupTimeS) {
            return new OffTimer(positionPath, null, followupTimeS * 1000);
        }

        public void start() {
            this.timer = System.currentTimeMillis();
        }

        public void reset() {
            this.timer = null;
        }

        public boolean within() {
            if (this.timer != null) {
                var span = (System.currentTimeMillis() - this.timer);
                Logger.debug(this.positionPath,"AUTO LIGHT OFF: TIMER MS: {} <= {}", span, this.followupTimeMS);
                return span <= this.followupTimeMS;
            }
            return true;
        }
    }

}