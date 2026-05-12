package tools.vlab.kberry.server.scheduler;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.KNXDevices;
import tools.vlab.kberry.core.mqtt.custom.devices.CustomMqttDevices;
import tools.vlab.kberry.core.mqtt.shelly.devices.ShellyDevices;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;
import tools.vlab.kberry.server.serviceProvider.ServiceProviders;

import java.util.function.Function;

public interface Schedule {
    void registerSchedule(PositionPath path, String id, Trigger trigger, Runnable logic);
    void unregister(PositionPath path, String id);

    void unregister(Scheduler scheduler);

    void registerSchedule(PositionPath path, String id, Trigger trigger, Runnable logic, Function<Integer, Boolean> checkStatus, int retry);

    void registerSchedule(KNXDevices devices, CustomMqttDevices mqttDevices, ShellyDevices shellyDevices, ServiceProviders serviceProviders, Scheduler scheduler);
}
