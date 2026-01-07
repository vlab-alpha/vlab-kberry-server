package tools.vlab.kberry.server.logic;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.vlab.kberry.core.PositionPath;
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
    private final ConcurrentHashMap<String, Periodic> periodics = new ConcurrentHashMap<>();

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
            startPeriodic(onOffDevice);
        } else {
            stopPeriodic(onOffDevice);
        }
    }

    @Override
    public void presenceChanged(PresenceSensor sensor, boolean available) {
        if (!contains(sensor.getPositionPath())) return;

        if (this.periodics.containsKey(sensor.getPositionPath().getId())) {
            if (available) {
                this.periodics.get(sensor.getPositionPath().getId()).resetTimer();
            } else {
                this.periodics.remove(sensor.getPositionPath().getId()).startTimer(followupTimeS);
            }
        }
    }

    private void startPeriodic(OnOffDevice device) {
        var timerId = this.getVertx().setPeriodic(1000, v -> {
            try {
                var periodic = periodics.get(device.getPositionPath().getId());
                if (device.isOn() && !periodic.within()) {
                    Log.info("Switching off light room {}",device.getPositionPath().getRoom());
                    device.off();
                    stopPeriodic(device);
                }
            } catch (Exception e) {
                Log.error("Check Periodic Presence failed", e);
            }
        });
        periodics.put(device.getPositionPath().getId(), Periodic.init(timerId, device.getPositionPath()));
    }

    private void stopPeriodic(OnOffDevice device) {
        var periodic = periodics.get(device.getPositionPath().getId());
        if (periodic != null) {
            getVertx().cancelTimer(periodic.getTimerId());
            periodics.remove(periodic.getPositionPath().getId());
        }
    }

    @Override
    public void stop() {
        this.periodics.values().forEach(periodic -> this.getVertx().cancelTimer(periodic.getTimerId()));
    }

    @Override
    public void start() {
        // Ignore
    }

    static final class Periodic {
        @Getter
        private final long timerId;
        @Getter
        private final PositionPath positionPath;
        private long timer;
        @Getter
        private Long followupTimeMs = null;

        private Periodic(long timerId, PositionPath positionPath) {
            this.timerId = timerId;
            this.positionPath = positionPath;
        }

        public static Periodic init(long timerId, PositionPath positionPath) {
            return new Periodic(timerId, positionPath);
        }

        public void startTimer(long followupTimeS) {
            this.timer = System.currentTimeMillis();
            this.followupTimeMs = followupTimeS * 1000;
        }

        public void resetTimer() {
            this.followupTimeMs = null;
        }

        public boolean within() {
            return followupTimeMs == null || (System.currentTimeMillis()) - timer <= followupTimeMs;
        }

    }

}