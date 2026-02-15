package tools.vlab.kberry.server.scheduler;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;

public record TriggerTask(PositionPath positionPath, String id, Trigger trigger, Runnable task) {

}
