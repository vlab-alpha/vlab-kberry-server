package tools.vlab.kberry.server.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduleEngine implements Schedule {

    private static final Logger Log = LoggerFactory.getLogger(ScheduleEngine.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, TriggerTask> schedules = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File persistenceFile;

    public ScheduleEngine(File persistenceFile) {
        this.persistenceFile = persistenceFile;
        loadPersistedSchedules();
    }

    /**
     * Start a task with a trigger and persist it.
     */
    public void start(String id, Trigger trigger, Runnable task) {
        TriggerTask triggerTask = new TriggerTask(trigger, task);
        schedules.put(id, triggerTask);
        persistSchedules();
        executor.scheduleAtFixedRate(() -> {
            if (trigger.matches(java.time.LocalDateTime.now())) {
                task.run();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void stop(String id) {
        schedules.remove(id);
        persistSchedules();
    }

    /**
     * Persist all schedules to JSON
     */
    private void persistSchedules() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(persistenceFile, schedules);
        } catch (IOException e) {
            Log.error("Store schedule failed!", e);
        }
    }

    /**
     * Load persisted schedules
     */
    private void loadPersistedSchedules() {
        if (!persistenceFile.exists()) return;

        try {
            Map<String, TriggerTask> persisted = objectMapper.readValue(
                    persistenceFile,
                    new TypeReference<>() {}
            );

            persisted.forEach((id, task) -> {
                schedules.put(id, task);
                executor.scheduleAtFixedRate(() -> {
                    if (task.trigger().matches(java.time.LocalDateTime.now())) {
                        task.runnable().run();
                    }
                }, 0, 1, TimeUnit.SECONDS);
            });

        } catch (IOException e) {
            Log.error("Load schedule failed!", e);
        }
    }
}
