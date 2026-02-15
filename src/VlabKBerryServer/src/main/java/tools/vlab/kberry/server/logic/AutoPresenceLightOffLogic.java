package tools.vlab.kberry.server.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.actor.Light;
import tools.vlab.kberry.core.devices.actor.OnOffDevice;
import tools.vlab.kberry.core.devices.actor.OnOffStatus;
import tools.vlab.kberry.core.devices.sensor.PresenceSensor;
import tools.vlab.kberry.core.devices.sensor.PresenceStatus;
import tools.vlab.kberry.server.log.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class AutoPresenceLightOffLogic extends Logic implements OnOffStatus, PresenceStatus {

    public final static String LOGIC_NAME = "AutoPresenceOff";

    private final int followupTimeS;
    private final ConcurrentHashMap<String, OffTimer> presence = new ConcurrentHashMap<>();
    private Long timerId = null;

    private AutoPresenceLightOffLogic(int followupTimeS, PositionPath pathOfLight) {
        super(LOGIC_NAME, pathOfLight);
        this.followupTimeS = followupTimeS;
    }

    public static AutoPresenceLightOffLogic at(int followupTimeS, PositionPath pathOfLight) {
        return new AutoPresenceLightOffLogic(followupTimeS, pathOfLight);
    }

    @Override
    public void onOffStatusChanged(OnOffDevice onOffDevice, boolean isOn) {
        if (isNotSamePosition(onOffDevice)) {
            Logger.debug(onOffDevice.getPositionPath(), "Skip to check off logic [Status:{}]", isOn);
            return;
        }

        if (isOn) {
            Logger.debug(onOffDevice.getPositionPath(), "Init Timer");
            presence.put(onOffDevice.getPositionPath().getRoom(), OffTimer.init(onOffDevice.getPositionPath(), followupTimeS));
        } else {
            Logger.debug(onOffDevice.getPositionPath(),"Remove Timer");
            presence.remove(onOffDevice.getPositionPath().getRoom());
        }
    }

    @Override
    public void presenceChanged(PresenceSensor sensor, boolean available) {
        if (isNotSameRoom(sensor)) return;

        Logger.debug(sensor.getPositionPath(), "Check to switch off light [PRESENCE:{}]", available);
        if (presence.containsKey(sensor.getPositionPath().getRoom())) {
            if (available) {
                Logger.debug(sensor.getPositionPath(), "Switch off light [Timer RESET]");
                presence.get(sensor.getPositionPath().getRoom()).reset();
            } else {
                Logger.debug(sensor.getPositionPath(), "Switch off light [Timer START]");
                presence.get(sensor.getPositionPath().getRoom()).start();
            }
        } else {
            Logger.debug(sensor.getPositionPath(),"Room does not have switch off logic!");
        }
    }

    private void startPeriodic() {
        timerId = this.getVertx().setPeriodic(5000, v -> {
            for (String room : presence.keySet()) {
                var timer = presence.get(room);
                if (!timer.within()) {
                    try {
                        Logger.debug(timer.getPositionPath(), "Switch off light");
                        this.getKnxDevices().getKNXDeviceByRoom(Light.class, timer.getPositionPath()).ifPresent(Light::off);
                    } catch (Exception e) {
                        Logger.error(timer.getPositionPath(), e,"Check Periodic Presence failed!");
                    }
                }
            }
        });
    }

    private void checkCurrentLights() {
        this.getKnxDevices().getKNXDevice(Light.class, this.getPositionPath())
                .filter(Light::isOn)
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
                Logger.debug(this.positionPath,"TIMER MS: {} <= {}", span, this.followupTimeMS);
                return span <= this.followupTimeMS;
            }
            return true;
        }
    }

}