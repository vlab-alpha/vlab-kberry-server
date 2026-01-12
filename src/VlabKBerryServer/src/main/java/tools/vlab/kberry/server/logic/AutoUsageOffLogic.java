package tools.vlab.kberry.server.logic;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.actor.OnOffDevice;
import tools.vlab.kberry.core.devices.actor.OnOffStatus;

import java.util.concurrent.*;

/**
 * Switched of in the specific time.
 */
public class AutoUsageOffLogic extends Logic implements OnOffStatus {

    private final int maxUsageMinutes;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<OnOffDevice, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    private AutoUsageOffLogic(PositionPath path, int maxUsageMinutes) {
        super(path);
        this.maxUsageMinutes = maxUsageMinutes;
    }

    public static AutoUsageOffLogic at(int maxUsageMinutes, PositionPath path) {
        return new AutoUsageOffLogic(path, maxUsageMinutes);
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
        if (!isSamePosition(device)) {
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