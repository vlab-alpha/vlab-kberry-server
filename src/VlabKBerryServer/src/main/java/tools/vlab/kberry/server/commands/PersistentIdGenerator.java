package tools.vlab.kberry.server.commands;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PersistentIdGenerator {

    private final File persistFile;
    private final AtomicInteger counter = new AtomicInteger(1);
    private final Map<String, Integer> nameToId = new HashMap<>();

    public PersistentIdGenerator(File persistFile) {
        this.persistFile = persistFile;
        load();
    }

    public synchronized int getId(Class<?> clazz, String name) {
        String key = clazz.getName() + ":" + name;
        return nameToId.computeIfAbsent(key, k -> {
            int id = counter.getAndIncrement();
            save();
            return id;
        });
    }

    private synchronized void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(persistFile))) {
            out.writeObject(counter.get());
            out.writeObject(nameToId);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist ID generator state", e);
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void load() {
        if (!persistFile.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(persistFile))) {
            counter.set((Integer) in.readObject());
            Map<String, Integer> loadedMap = (Map<String, Integer>) in.readObject();
            nameToId.clear();
            nameToId.putAll(loadedMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ID generator state", e);
        }
    }
}
