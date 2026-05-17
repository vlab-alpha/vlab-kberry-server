package tools.vlab.kberry.server.scheduler;

import lombok.Getter;
import lombok.Setter;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.KNXDevices;
import tools.vlab.kberry.core.mqtt.custom.devices.CustomMqttDevices;
import tools.vlab.kberry.core.mqtt.shelly.devices.ShellyDevices;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;
import tools.vlab.kberry.server.serviceProvider.ServiceProviders;

public abstract class Scheduler {

    @Setter
    @Getter
    private ScheduleEngine engine;

    public abstract Trigger getTrigger();

    public abstract void executed(KNXDevices devices, CustomMqttDevices mqttDevices, ShellyDevices shellyDevices, ServiceProviders serviceProviders);

    public abstract String getTaskId();

    public abstract PositionPath getPositionPath();

    public int getRetry() {
        return 1;
    }

    public Boolean checkStatus(Integer retry, KNXDevices devices, CustomMqttDevices mqttDevices, ShellyDevices shellyDevices, ServiceProviders serviceProviders) {
        return true;
    }
}
