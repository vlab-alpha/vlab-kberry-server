package tools.vlab.kberry.server.logic;

import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.server.serviceProvider.ServiceProviders;
import tools.vlab.kberry.server.statistics.Statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class Logics {

    private final Vertx vertx;
    private final KNXDevices knxDevices;
    @Getter
    private final ServiceProviders services;
    @Getter
    private final List<Logic> logicList = new ArrayList<>();
    private final Statistics statistics;


    public Logics(Vertx vertx, KNXDevices knxDevices, ServiceProviders services, Statistics statistics) {
        this.vertx = vertx;
        this.knxDevices = knxDevices;
        this.services = services;
        this.statistics = statistics;
    }

    public String register(Logic logic) {
        logic.setKnxDevices(knxDevices);
        logic.setServiceProviders(services);
        logic.setStatistics(statistics);
        logic.start(this.vertx);
        knxDevices.getAllDevices().forEach(device -> device.addListener(logic));
        logicList.add(logic);
        return logic.getId();
    }

    public void unregister(Logic logic) {
        knxDevices.getAllDevices().forEach(device -> device.RemoveListener(logic));
        logicList.remove(logic);
    }

    public void unregister(String logicId) {
        this.getLogic(logicId)
                .ifPresent(logicList::remove);
    }

    public Optional<Logic> getLogic(String logicId) {
        return this.logicList.stream()
                .filter(l -> l.getId().equalsIgnoreCase(logicId))
                .findFirst();
    }

    public void stop() {
        this.logicList.forEach(Logic::stop);
    }


}
