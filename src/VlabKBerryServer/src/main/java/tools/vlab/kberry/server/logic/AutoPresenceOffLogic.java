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

public class AutoPresenceOffLogic extends Logic implements OnOffStatus, PresenceStatus {

    private static final Logger Log = LoggerFactory.getLogger(AutoPresenceOffLogic.class);

    private final int followupTimeS;
    private final ConcurrentHashMap<String, OffTimer> presence = new ConcurrentHashMap<>();
    private Long timerId = null;

    private AutoPresenceOffLogic(int followupTimeS, PositionPath pathOfLight) {
        super(pathOfLight);
        this.followupTimeS = followupTimeS;
    }

    public static AutoPresenceOffLogic at(int followupTimeS, PositionPath pathOfLight) {
        return new AutoPresenceOffLogic(followupTimeS, pathOfLight);
    }

    @Override
    public void onOffStatusChanged(OnOffDevice onOffDevice, boolean isOn) {
        if (!isSamePosition(onOffDevice)) {
            Log.info("Ignore {}",onOffDevice.getPositionPath());
            return;
        }

        if (isOn) {
            Log.info("Init Timer  {}",onOffDevice.getPositionPath());
            presence.put(onOffDevice.getPositionPath().getRoom(), OffTimer.init(onOffDevice.getPositionPath(), followupTimeS));
        } else {
            Log.info("Remove Timer for {}",onOffDevice.getPositionPath());
            presence.remove(onOffDevice.getPositionPath().getRoom());
        }
    }

    @Override
    public void presenceChanged(PresenceSensor sensor, boolean available) {
        if (!isSameRoom(sensor)) return;

        Log.info("SWITCH OFF Presence {}",sensor.getPositionPath());
        if (presence.containsKey(sensor.getPositionPath().getRoom())) {
            Log.info("Presence change from room: {} {}", sensor.getPositionPath().getRoom(), available);
            if (available) {
                presence.get(sensor.getPositionPath().getRoom()).reset();
            } else {
                Log.info("Timer start for room {}", sensor.getPositionPath().getRoom());
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
                        Log.info("Switch off light for room: {}", room);
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