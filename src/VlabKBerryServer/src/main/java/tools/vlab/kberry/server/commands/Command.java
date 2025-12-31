package tools.vlab.kberry.server.commands;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.server.logic.Logics;
import tools.vlab.kberry.server.scheduler.Schedule;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;
import tools.vlab.kberry.server.serviceProvider.ServiceProviders;
import tools.vlab.kberry.server.statistics.Statistics;

import java.io.File;
import java.util.Optional;

@Getter
@Setter(AccessLevel.PROTECTED)
public abstract class Command {

    private KNXDevices knxDevices;
    private Logics logics;
    private Statistics statistics;
    private ServiceProviders serviceProviders;
    private Schedule schedule;
    private final PersistentIdGenerator idGenerator = new PersistentIdGenerator(new File(this.topic().getIdPath()));

    public abstract Future<Optional<JsonObject>> execute(JsonObject message);

    public abstract CommandTopic topic();

    public <T> int getScheduleId(Class<T> tClass, String name) {
        return idGenerator.getId(tClass, name);
    }

    public String getMqttTopic() {
        return "command/" + topic().getTopic();
    }

    public void start(String id, Trigger trigger, Runnable task) {
        schedule.start(id, trigger, task);
    }

    public void stop(String id) {
        schedule.stop(id);
    }


}
