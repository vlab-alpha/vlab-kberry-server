package tools.vlab.kberry.server.scheduler;

import lombok.Getter;
import lombok.Setter;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;

import java.time.LocalDateTime;
import java.util.function.Function;

@Getter
public final class TriggerTask {

    private final PositionPath positionPath;
    private final String id;
    private final Trigger trigger;
    private final Runnable task;
    private final Function<Integer, Boolean> checkStatus;
    private final int retry;
    private final int retryS;
    @Setter
    private boolean running;
    private int retryCounter;
    @Setter
    private LocalDateTime nextCheck;

    public TriggerTask(PositionPath positionPath, String id, Trigger trigger, Runnable task, Function<Integer, Boolean> checkStatus, int retry, LocalDateTime nextCheck, int retryS) {
        this.positionPath = positionPath;
        this.id = id;
        this.trigger = trigger;
        this.task = task;
        this.checkStatus = checkStatus;
        this.retry = retry;
        this.nextCheck = nextCheck;
        this.retryCounter = retry;
        this.retryS = retryS;
    }

    public static TriggerTask once(PositionPath positionPath, String id, Trigger trigger, Runnable task) {
        return new TriggerTask(positionPath, id, trigger, task, null, 0, null, 0);
    }

    public static TriggerTask retry(PositionPath positionPath, String id, Trigger trigger, Runnable task, Function<Integer, Boolean> checkStatus, int retry) {
        return new TriggerTask(positionPath, id, trigger, task, checkStatus, retry, null, 5);
    }

    public static TriggerTask retry(PositionPath positionPath, String id, Trigger trigger, Runnable task, Function<Integer, Boolean> checkStatus, int retry, int retryS) {
        return new TriggerTask(positionPath, id, trigger, task, checkStatus, retry, null, retryS);
    }

    public void decrementRetry() {
        retryCounter = retryCounter - 1;
    }

    public void resetRetry() {
        retryCounter = retry;
    }

    public void stopNextCheck() {
        nextCheck = null;
        resetRetry();
    }

}
