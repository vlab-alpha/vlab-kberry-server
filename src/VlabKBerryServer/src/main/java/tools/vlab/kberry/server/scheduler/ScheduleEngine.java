package tools.vlab.kberry.server.scheduler;

import io.vertx.core.AbstractVerticle;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.KNXDevices;
import tools.vlab.kberry.core.mqtt.custom.devices.CustomMqttDevices;
import tools.vlab.kberry.core.mqtt.shelly.devices.ShellyDevices;
import tools.vlab.kberry.server.log.Logger;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;
import tools.vlab.kberry.server.serviceProvider.ServiceProviders;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ScheduleEngine extends AbstractVerticle implements Schedule {

    private final Map<String, TriggerTask> scheduleMap = new ConcurrentHashMap<>();
    private Long timerId;


    public ScheduleEngine() {
    }

    @Override
    public void start() {
        Logger.info("Scheduler Start ...");
        timerId = vertx.setPeriodic(1000, fireId -> scheduleMap.values().forEach(triggerTasks -> {
            var now = LocalDateTime.now();
            if (triggerTasks.getTrigger().matches(now) && !triggerTasks.isRunning()) {
                triggerTasks.setRunning(true);
                try {
                    Logger.info(triggerTasks.getPositionPath(), "SCHEDULER: Execute Task T:{} P:{}", triggerTasks.getTrigger(), triggerTasks.getId());
                    triggerTasks.getTask().run();
                    triggerTasks.setRunning(false);

                    if (triggerTasks.getCheckStatus() != null && triggerTasks.getRetry() > 0) {
                        triggerTasks.setNextCheck(now.plusSeconds(5)); // Beispiel: 5 Sekunden später
                        triggerTasks.resetRetry();
                    } else if(triggerTasks.getTrigger().isOnce()) {
                        scheduleMap.remove(triggerTasks.getId());
                    }
                } catch (Exception e) {
                    Logger.error(triggerTasks.getPositionPath(), e, "SCHEDULER: Error executing task {}", triggerTasks.getId());
                }
            }

            if (triggerTasks.getCheckStatus() != null && triggerTasks.getNextCheck() != null && !now.isBefore(triggerTasks.getNextCheck())) {
                boolean ok = Boolean.TRUE.equals(
                        triggerTasks.getCheckStatus().apply(triggerTasks.getRetryCounter())
                );
                if (!ok) {

                    if (triggerTasks.getRetryCounter() > 0) {

                        Logger.info(triggerTasks.getPositionPath(),
                                "SCHEDULER: Retry {} for {}",
                                triggerTasks.getRetryCounter(),
                                triggerTasks.getId());
                        triggerTasks.setRunning(true);
                        triggerTasks.getTask().run();
                        triggerTasks.setRunning(false);
                        triggerTasks.decrementRetry();
                        triggerTasks.setNextCheck(now.plusSeconds(5));
                    } else {
                        triggerTasks.stopNextCheck();
                         if(triggerTasks.getTrigger().isOnce()) {
                            scheduleMap.remove(triggerTasks.getId());
                        }
                    }
                } else {
                    triggerTasks.stopNextCheck();
                    if(triggerTasks.getTrigger().isOnce()) {
                        scheduleMap.remove(triggerTasks.getId());
                    }
                }
            }
        }));
    }

    public void registerSchedule(PositionPath path, String taskId, Trigger trigger, Runnable logic) {
        var id = id(path, taskId);
        scheduleMap.put(id, TriggerTask.once(path, id, trigger, logic));
    }

    public void registerSchedule(KNXDevices devices, CustomMqttDevices mqttDevices, ShellyDevices shellyDevices, ServiceProviders serviceProviders, Scheduler scheduler) {
        scheduler.setEngine(this);
        scheduleMap.put(scheduler.getId(), TriggerTask.once(scheduler.getPositionPath(), scheduler.getId(), scheduler.getTrigger(), () -> scheduler.executed(devices, mqttDevices, shellyDevices, serviceProviders)));
    }

    @Override
    public void unregister(PositionPath path, String id) {
        scheduleMap.remove(id(path, id));
    }

    public void unregister(Scheduler scheduler) {
        scheduleMap.remove(scheduler.getId());
    }

    @Override
    public void registerSchedule(PositionPath path, String id, Trigger trigger, Runnable logic, Function<Integer, Boolean> checkStatus, int retry) {
        var scheduleId = id(path, id);
        scheduleMap.put(scheduleId,
                TriggerTask.retry(path, scheduleId, trigger, logic, checkStatus, retry));
    }

    private String id(PositionPath path, String taskId) {
        return String.format("%s.%s", path.getId(), taskId);
    }

    @Override
    public void stop() {
        if (timerId != null) {
            this.vertx.cancelTimer(timerId);
        }
        Logger.info("SCHEDULER: ScheduleEngine stopped...");
    }
}
