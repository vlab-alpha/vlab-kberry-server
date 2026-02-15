package tools.vlab.kberry.server.scheduler;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;

import java.util.UUID;

public abstract class Scheduler {

    public abstract Trigger getTrigger();

    public abstract void executed(KNXDevices knxDevices);

    protected String getId() {
        return UUID.randomUUID().toString();
    }

    public abstract PositionPath getPositionPath();

}
