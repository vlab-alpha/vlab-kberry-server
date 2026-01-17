package tools.vlab.kberry.server.scheduler;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;

public interface Schedule {
    void registerSchedule(PositionPath path, String id, Trigger trigger, Runnable logic);
    void unregister(PositionPath path, String id);
}
