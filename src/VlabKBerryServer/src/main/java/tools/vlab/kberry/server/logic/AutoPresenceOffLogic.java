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

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class AutoPresenceOffLogic extends Logic implements OnOffStatus, PresenceStatus {

    private static final Logger Log = LoggerFactory.getLogger(AutoPresenceOffLogic.class);

    private final int followupTimeS;
    private final ConcurrentHashMap<String, Timer> presence = new ConcurrentHashMap<>();
    private Long timerId = null;

    private AutoPresenceOffLogic(int followupTimeS, Vector<PositionPath> paths) {
        super(paths);
        this.followupTimeS = followupTimeS;
    }

    public static AutoPresenceOffLogic at(int followupTimeS, PositionPath... positionPath) {
        return new AutoPresenceOffLogic(followupTimeS, new Vector<>(List.of(positionPath)));
    }

    @Override
    public void onOffStatusChanged(OnOffDevice onOffDevice, boolean isOn) {
        if (!contains(onOffDevice.getPositionPath())) return;

        if (isOn) {
            presence.put(onOffDevice.getPositionPath().getRoom(), Timer.init(onOffDevice.getPositionPath(), followupTimeS));
        } else {
            presence.remove(onOffDevice.getPositionPath().getRoom());
        }
    }

    @Override
    public void presenceChanged(PresenceSensor sensor, boolean available) {
        if (!contains(sensor.getPositionPath())) return;

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
        getRequiredPositionPaths().forEach(p -> this.getKnxDevices().getKNXDevice(Light.class, p)
                .filter(Light::isOn)
                .ifPresent(lightOn -> presence.put(
                        lightOn.getPositionPath().getRoom(),
                        Timer.init(lightOn.getPositionPath(), followupTimeS))
                ));
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
    static class Timer {
        PositionPath positionPath;
        long timer;
        long followupTimeMS;

        public static Timer init(PositionPath positionPath, long followupTimeS) {
            return new Timer(positionPath, 0L, followupTimeS * 1000);
        }

        public void start() {
            this.timer = System.currentTimeMillis();
        }

        public void reset() {
            this.timer = 0L;
        }

        public boolean within() {
            var span = (System.currentTimeMillis() - this.timer);
            Log.debug("TIMER MS: {} <= {} for room {}", span, this.followupTimeMS, this.positionPath.getRoom());
            return span <= this.followupTimeMS;
        }
    }

}