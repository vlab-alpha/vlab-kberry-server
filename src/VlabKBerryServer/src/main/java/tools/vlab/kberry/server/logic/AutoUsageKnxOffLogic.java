package tools.vlab.kberry.server.logic;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.actor.OnOffDevice;
import tools.vlab.kberry.core.knx.devices.actor.OnOffStatus;

import java.util.concurrent.*;

/**
 * Switched of in the specific time.
 */
public class AutoUsageKnxOffLogic extends Logic implements OnOffStatus {

    public final static String LOGIC_NAME = "AutoKnxUsageOff";

    private final int maxUsageMinutes;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<OnOffDevice, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    private AutoUsageKnxOffLogic(PositionPath path, int maxUsageMinutes) {
        super(LOGIC_NAME, path);
        this.maxUsageMinutes = maxUsageMinutes;
    }

    public static AutoUsageKnxOffLogic at(int maxUsageMinutes, PositionPath path) {
        return new AutoUsageKnxOffLogic(path, maxUsageMinutes);
    }

    @Override
    public void stop() {
        executor.shutdownNow();
        activeTasks.clear();
    }

    @Override
    public void start() {
    }

    @Override
    public void onOffStatusChanged(OnOffDevice device, boolean isOn) {
        if (isNotSamePosition(device)) {
            return;
        }

        if (!isOn) {
            ScheduledFuture<?> oldTask = activeTasks.remove(device);
            if (oldTask != null) {
                oldTask.cancel(false);
            }
            return;
        }

        ScheduledFuture<?> oldTask = activeTasks.remove(device);
        if (oldTask != null) {
            oldTask.cancel(false);
        }

        ScheduledFuture<?> task = executor.schedule(() -> {
            device.off();
            activeTasks.remove(device);
        }, maxUsageMinutes, TimeUnit.MINUTES);

        activeTasks.put(device, task);
    }
}