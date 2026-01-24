package tools.vlab.kberry.server.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.actor.Light;
import tools.vlab.kberry.core.devices.actor.OnOffDevice;
import tools.vlab.kberry.core.devices.actor.OnOffStatus;
import tools.vlab.kberry.core.devices.sensor.PresenceSensor;
import tools.vlab.kberry.core.devices.sensor.PresenceStatus;

import java.util.concurrent.ConcurrentHashMap;

public class AutoPresenceLightOffLogic extends Logic implements OnOffStatus, PresenceStatus {

    private static final Logger Log = LoggerFactory.getLogger(AutoPresenceLightOffLogic.class);
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
            Log.debug("Ignore {}", onOffDevice.getPositionPath());
            return;
        }

        if (isOn) {
            Log.debug("Init Timer  {}", onOffDevice.getPositionPath());
            presence.put(onOffDevice.getPositionPath().getRoom(), OffTimer.init(onOffDevice.getPositionPath(), followupTimeS));
        } else {
            Log.debug("Remove Timer for {}", onOffDevice.getPositionPath());
            presence.remove(onOffDevice.getPositionPath().getRoom());
        }
    }

    @Override
    public void presenceChanged(PresenceSensor sensor, boolean available) {
        if (isNotSameRoom(sensor)) return;

        Log.debug("SWITCH OFF Presence {}", sensor.getPositionPath());
        if (presence.containsKey(sensor.getPositionPath().getRoom())) {
            Log.debug("Presence change from room: {} {}", sensor.getPositionPath().getRoom(), available);
            if (available) {
                presence.get(sensor.getPositionPath().getRoom()).reset();
            } else {
                Log.debug("Timer start for room {}", sensor.getPositionPath().getRoom());
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
                        Log.debug("Switch off light for room: {}", room);
                        this.getKnxDevices().getKNXDeviceByRoom(Light.class, timer.getPositionPath()).ifPresent(Light::off);
                    } catch (Exception e) {
                        Log.error("Check Periodic Presence failed for path {}!", timer.getPositionPath().getPath(), e);
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
                Log.debug("TIMER MS: {} <= {} for room {}", span, this.followupTimeMS, this.positionPath.getRoom());
                return span <= this.followupTimeMS;
            }
            return true;
        }
    }

}