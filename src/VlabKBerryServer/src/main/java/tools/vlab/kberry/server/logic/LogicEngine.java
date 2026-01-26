package tools.vlab.kberry.server.logic;

import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.server.serviceProvider.ServiceProviders;
import tools.vlab.kberry.server.statistics.Statistics;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LogicEngine {

    private final Vertx vertx;
    private final KNXDevices knxDevices;
    @Getter
    private final ServiceProviders services;
    @Getter
    private final ConcurrentHashMap<String, Logic> logicMap = new ConcurrentHashMap<>();
    private final Statistics statistics;


    public LogicEngine(Vertx vertx, KNXDevices knxDevices, ServiceProviders services, Statistics statistics) {
        this.vertx = vertx;
        this.knxDevices = knxDevices;
        this.services = services;
        this.statistics = statistics;
    }

    public void register(Logic logic) {
        logic.setKnxDevices(knxDevices);
        logic.setServiceProviders(services);
        logic.setStatistics(statistics);
        logic.start(this.vertx);
        if (logicMap.containsKey(logic.getId())) {
            log.info("Update Logic {} for room {}!", logic.getName(), logic.getPositionPath().getRoom());
            knxDevices.getAllDevices().forEach(device -> device.removeListener(logicMap.get(logic.getId())));
            logicMap.get(logic.getId()).stop();
            logicMap.put(logic.getId(), logic);
        } else {
            log.info("Add Logic {} for room {}!", logic.getName(), logic.getPositionPath().getRoom());
            logicMap.put(logic.getId(), logic);
        }
    }

    public void unregister(Logic logic) {
        if (logicMap.containsKey(logic.getId())) {
            log.info("Remove Logic {} for room {}!", logic.getName(), logic.getPositionPath().getRoom());
            logicMap.get(logic.getId()).stop();
            knxDevices.getAllDevices().forEach(device -> device.removeListener(logic));
            logicMap.remove(logic.getId());
        }
    }

    public void unregister(PositionPath path, String logicName) {
        this.getLogic(path, logicName)
                .ifPresent(this::unregister);
    }

    public Optional<Logic> getLogic(PositionPath path, String logicName) {
        var key = String.join("@", logicName, path.getId());
        if (this.logicMap.containsKey(key)) {
            return Optional.of(this.logicMap.get(key));
        }
        return Optional.empty();
    }

    public void stop() {
        this.logicMap.values().forEach(Logic::stop);
    }


    public List<String> getLogicNames(PositionPath path) {
        return this.logicMap.values().stream()
                .filter(logic -> logic.getPositionPath().isSame(path))
                .map(Logic::getName)
                .toList();
    }

}
