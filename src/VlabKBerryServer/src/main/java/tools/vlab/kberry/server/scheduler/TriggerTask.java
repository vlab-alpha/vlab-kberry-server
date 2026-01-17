package tools.vlab.kberry.server.scheduler;

import tools.vlab.kberry.server.scheduler.trigger.Trigger;

public record TriggerTask(String id, Trigger trigger, Runnable task) {

}
