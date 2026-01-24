package tools.vlab.kberry.server.logic;

import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.server.serviceProvider.ServiceProviders;
import tools.vlab.kberry.server.statistics.Statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class LogicEngine {

    private final Vertx vertx;
    private final KNXDevices knxDevices;
    @Getter
    private final ServiceProviders services;
    @Getter
    private final List<Logic> logicList = new ArrayList<>();
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
        knxDevices.getAllDevices().forEach(device -> device.addListener(logic));
        if (logicList.stream().noneMatch(l -> l.hasId(logic.getPositionPath(), logic.getName()))) {
            logicList.add(logic);
            log.info("Add Logic {} for room {}!", logic.getName(), logic.getPositionPath().getRoom());
        } else {
            log.error("Ignore Logic {} for room {}!", logic.getName(), logic.getPositionPath().getRoom());
        }
    }

    public void unregister(Logic logic) {
        knxDevices.getAllDevices().forEach(device -> device.RemoveListener(logic));
        logicList.remove(logic);
    }

    public void unregister(PositionPath path, String logicName) {
        this.getLogic(path, logicName)
                .ifPresent(logicList::remove);
    }

    public Optional<Logic> getLogic(PositionPath path, String logicName) {
        return this.logicList.stream()
                .filter(l -> l.hasId(path, logicName))
                .findFirst();
    }

    public void stop() {
        this.logicList.forEach(Logic::stop);
    }


    public List<String> getLogicNames(PositionPath path) {
        return this.logicList.stream()
                .filter(logic -> logic.getPositionPath().isSame(path))
                .map(Logic::getName)
                .toList();
    }

}
