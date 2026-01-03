package tools.vlab.kberry.server.logic;

import lombok.Getter;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.actor.OnOffDevice;
import tools.vlab.kberry.core.devices.actor.OnOffStatus;
import tools.vlab.kberry.core.devices.sensor.PresenceSensor;
import tools.vlab.kberry.core.devices.sensor.PresenceStatus;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class AutoPresenceOffLogic extends Logic implements OnOffStatus, PresenceStatus {

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

        if (available && this.periodics.containsKey(sensor.getPositionPath().getId())) {
            this.periodics.get(sensor.getPositionPath().getId()).resetTimer();
        }
    }

    private void startPeriodic(OnOffDevice device) {
        var timerId = this.getVertx().setPeriodic(1000, _ -> {
            var periodic = periodics.get(device.getPositionPath().getId());
            if (device.isOn() && !periodic.within()) {
                device.off();
                stopPeriodic(device);
            }
        });
        periodics.put(device.getPositionPath().getId(), new Periodic(timerId, device.getPositionPath(), followupTimeS));
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
        private final long followupTimeMs;

        private Periodic(long timerId, PositionPath positionPath, long followupTimeMs) {
            this.timerId = timerId;
            this.positionPath = positionPath;
            this.timer = System.currentTimeMillis();
            this.followupTimeMs = followupTimeMs;
        }

        public static Periodic start(long timerId, PositionPath positionPath, long followupTimeS) {
            return new Periodic(timerId, positionPath, followupTimeS * 1000);
        }

        public void resetTimer() {
            timer = System.currentTimeMillis();
        }

        public boolean within() {
            return (System.currentTimeMillis()) - timer <= followupTimeMs;
        }

    }

}