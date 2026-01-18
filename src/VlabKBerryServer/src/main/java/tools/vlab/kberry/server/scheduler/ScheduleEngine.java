package tools.vlab.kberry.server.scheduler;

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScheduleEngine extends AbstractVerticle implements Schedule {

    private static final Logger Log = LoggerFactory.getLogger(ScheduleEngine.class);
    private final Map<String, TriggerTask> scheduleMap = new ConcurrentHashMap<>();
    private Long timerId;

    public ScheduleEngine() {
    }

    @Override
    public void start() {
        Log.info("Scheduler Start ...");
        timerId = vertx.setPeriodic(1000, fireId -> scheduleMap.values().forEach(triggerTasks -> {
            if (triggerTasks.trigger().matches(LocalDateTime.now())) {
                try {
                    Log.info("Execute Task T:{} P:{}", triggerTasks.trigger(),triggerTasks.id());
                    triggerTasks.task().run();
                } catch (Exception e) {
                    Log.error("Error executing task {}", triggerTasks.id(), e);
                }
            }
        }));
    }

    public void registerSchedule(PositionPath path, String taskId, Trigger trigger, Runnable logic) {
        var id = id(path, taskId);
        scheduleMap.put(id, new TriggerTask(id, trigger, logic));
    }

    public void registerSchedule(KNXDevices knxDevices, Scheduler scheduler) {
        scheduleMap.put(scheduler.getId(), new TriggerTask(scheduler.getId(), scheduler.getTrigger(), () -> scheduler.executed(knxDevices)));
    }

    @Override
    public void unregister(PositionPath path, String id) {
        scheduleMap.remove(id(path, id));
    }

    private String id(PositionPath path, String taskId) {
        return String.format("%s.%s", path.getId(), taskId);
    }

    @Override
    public void stop() {
        if (timerId != null) {
            this.vertx.cancelTimer(timerId);
        }
        Log.info("ScheduleEngine stopped.");
    }
}
