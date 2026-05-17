package tools.vlab.kberry.server.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.actor.OnOffDevice;
import tools.vlab.kberry.core.knx.devices.actor.OnOffStatus;
import tools.vlab.kberry.core.knx.devices.sensor.PresenceSensor;
import tools.vlab.kberry.core.knx.devices.sensor.PresenceStatus;
import tools.vlab.kberry.server.log.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// TODO: next version, es wird kein false event benötigt, sondern es soll nur abgelegt, wann der sensor das letzte mal eine Bewegung erkannt hat
public class AutoPresenceKnxOffLogic<T extends OnOffDevice> extends Logic implements OnOffStatus, PresenceStatus {

    public final static String LOGIC_NAME = "AutoKnxPresenceOff";

    private final int followupTimeS;
    private final ConcurrentHashMap<String, OffTimer> roomTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> activeSensors = new ConcurrentHashMap<>();
    private Long timerId = null;
    private final Class<T> tClass;

    private AutoPresenceKnxOffLogic(Class<T> tClass, int followupTimeS, PositionPath pathOfLight) {
        super(LOGIC_NAME, pathOfLight);
        this.followupTimeS = followupTimeS;
        this.tClass = tClass;
    }

    public static <T extends OnOffDevice> AutoPresenceKnxOffLogic<T> at(Class<T> tClass, int followupTimeS, PositionPath pathOfLight) {
        return new AutoPresenceKnxOffLogic<T>(tClass, followupTimeS, pathOfLight);
    }

    @Override
    public void onOffStatusChanged(OnOffDevice onOffDevice, boolean isOn) {
        if (isNotSamePosition(onOffDevice)) {
            Logger.debug(onOffDevice, "AUTO LIGHT OFF: SKIP Device does not have logic [Status:{}]", isOn);
            return;
        }
        var room = onOffDevice.getPositionPath().getRoom();

        if (isOn) {
            Logger.debug(onOffDevice, "AUTO LIGHT OFF: Init Timer");
            roomTimers.put(room, OffTimer.init(onOffDevice.getPositionPath(), followupTimeS));
        } else {
            Logger.debug(onOffDevice, "AUTO LIGHT OFF: Remove Timer");
            roomTimers.remove(room);
            activeSensors.remove(room);
        }
    }

    @Override
    public void presenceChanged(PresenceSensor sensor, boolean available) {
        if (isNotSameRoom(sensor)) return;

        String room = sensor.getPositionPath().getRoom();
        activeSensors.putIfAbsent(room, ConcurrentHashMap.newKeySet());
        var sensors = activeSensors.get(room);
        String sensorId = sensor.getPositionPath().toString();
        if (available) {
            Logger.debug(sensor, "AUTO LIGHT OFF: Sensor ACTIVE [{}]", sensorId);
            sensors.add(sensorId);
            if (roomTimers.containsKey(room)) {
                roomTimers.get(room).reset();
            }
        } else {
            Logger.debug(sensor, "AUTO LIGHT OFF: Sensor INACTIVE [{}]", sensorId);
            sensors.remove(sensorId);
            if (sensors.isEmpty()) {
                Logger.debug(sensor, "AUTO LIGHT OFF: No active sensors -> start timer");
                if (roomTimers.containsKey(room)) {
                    roomTimers.get(room).start();
                }
            }
        }
    }

    private void startPeriodic() {
        timerId = this.getVertx().setPeriodic(5000, v -> {
            for (String room : roomTimers.keySet()) {
                var timer = roomTimers.get(room);
                if (!timer.within()) {
                    try {
                        this.getKnxDevices().getKNXDeviceByRoom(this.tClass, timer.getPositionPath()).ifPresent(light -> {
                            Logger.debug(light, "AUTO LIGHT OFF: is not in the time range, switch off the light");
                            light.off();
                        });
                    } catch (Exception e) {
                        Logger.error(timer.getPositionPath(), e, "AUTO LIGHT OFF: Check Periodic Presence failed!");
                    }
                }
            }
        });
    }

    private void checkCurrentLights() {
        this.getKnxDevices().getKNXDevice(this.tClass, this.getPositionPath())
                .filter(OnOffDevice::isOn)
                .ifPresent(lightOn -> roomTimers.put(
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
            if (this.timer == null) {
                this.timer = System.currentTimeMillis();
            }
        }

        public void reset() {
            this.timer = null;
        }

        public boolean within() {
            if (this.timer != null) {
                var span = (System.currentTimeMillis() - this.timer);
                Logger.debug(this.positionPath, "AUTO LIGHT OFF: TIMER MS: {} <= {}", span, this.followupTimeMS);
                return span <= this.followupTimeMS;
            }
            return true;
        }
    }

}