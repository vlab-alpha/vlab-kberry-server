package tools.vlab.kberry.server.scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskRegistry {

    private final Map<String, Runnable> tasks = new ConcurrentHashMap<>();

    public void register(String taskId, Runnable task) {
        tasks.put(taskId, task);
    }

    public Runnable get(String taskId) {
        Runnable r = tasks.get(taskId);
        if (r == null) {
            throw new IllegalStateException("No task registered for id " + taskId);
        }
        return r;
    }
}
