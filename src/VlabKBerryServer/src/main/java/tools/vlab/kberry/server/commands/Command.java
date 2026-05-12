package tools.vlab.kberry.server.commands;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.KNXDevices;
import tools.vlab.kberry.core.mqtt.custom.devices.CustomMqttDevices;
import tools.vlab.kberry.core.mqtt.shelly.devices.ShellyDevices;
import tools.vlab.kberry.server.logic.LogicEngine;
import tools.vlab.kberry.server.scheduler.Schedule;
import tools.vlab.kberry.server.scheduler.Scheduler;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;
import tools.vlab.kberry.server.serviceProvider.ServiceProviders;
import tools.vlab.kberry.server.statistics.Statistics;

import java.io.File;
import java.util.Optional;
import java.util.function.Function;

@Getter
@Setter(AccessLevel.PROTECTED)
public abstract class Command {

    private KNXDevices knxDevices;
    private CustomMqttDevices mqttDevices;
    private ShellyDevices shellyDevices;
    private LogicEngine logicEngine;
    private Statistics statistics;
    private ServiceProviders serviceProviders;
    private Schedule schedule;
    private final PersistentIdGenerator idGenerator = new PersistentIdGenerator(new File(this.topic().getIdPath()));

    public abstract Future<Optional<JsonObject>> execute(JsonObject message);

    public abstract CommandTopic topic();

    public <T> int getScheduleId(Class<T> tClass, String name) {
        return idGenerator.getId(tClass, name);
    }

    public String getTopic() {
        return topic().getTopic();
    }

    public String getMqttTopic() {
        return "command/" + topic().getTopic();
    }

    public void register(PositionPath positionPath, String taskId, Trigger trigger, Runnable task) {
        schedule.registerSchedule(positionPath, taskId, trigger, task);
    }

    public void register(PositionPath positionPath, String taskId, Trigger trigger, Runnable logic, Function<Integer, Boolean> checkStatus, int retry) {
        schedule.registerSchedule(positionPath, taskId, trigger, logic, checkStatus, retry);
    }

    public void register(Scheduler scheduler) {
        schedule.registerSchedule(knxDevices, mqttDevices, shellyDevices, serviceProviders, scheduler);
    }

    public void unregister(PositionPath positionPath, String taskId) {
        schedule.unregister(positionPath, taskId);
    }

    public void unregister(Scheduler scheduler) {
        schedule.unregister(scheduler);
    }

    public abstract void init();


}
