package tools.vlab.kberry.server.scheduler.trigger;

import java.time.LocalDateTime;

public interface Trigger {

    /**
     * @return true if the trigger should fire at the given time
     */
    boolean matches(LocalDateTime now);


}
