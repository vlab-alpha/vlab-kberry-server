package tools.vlab.kberry.server.scheduler;

import tools.vlab.kberry.server.scheduler.trigger.Trigger;

public interface Schedule {
    void stop(String id);
    void start(String id, Trigger trigger, Runnable task);
}
