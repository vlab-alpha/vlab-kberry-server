package tools.vlab.kberry.server.logic;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.mqtt.shelly.devices.device.Plug;
import tools.vlab.kberry.core.mqtt.shelly.devices.device.PlugStatus;

import java.util.concurrent.*;

/**
 * Switched of in the specific time.
 */
public class AutoUsageShellyOffLogic extends Logic implements PlugStatus {

    public final static String LOGIC_NAME = "AutoShellyUsageOff";

    private final int maxUsageMinutes;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<Plug, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    private AutoUsageShellyOffLogic(PositionPath path, int maxUsageMinutes) {
        super(LOGIC_NAME, path);
        this.maxUsageMinutes = maxUsageMinutes;
    }

    public static AutoUsageShellyOffLogic at(int maxUsageMinutes, PositionPath path) {
        return new AutoUsageShellyOffLogic(path, maxUsageMinutes);
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
    public void isOnChanged(Plug device, Boolean isOn) {
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