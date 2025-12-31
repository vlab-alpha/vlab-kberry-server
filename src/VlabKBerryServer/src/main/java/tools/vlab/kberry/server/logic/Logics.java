package tools.vlab.kberry.server.logic;

import lombok.Getter;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.server.serviceProvider.ServiceProviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Logics {

    private final KNXDevices knxDevices;
    @Getter
    private final ServiceProviders services;
    @Getter
    private final List<Logic> logicList = new ArrayList<>();


    public Logics(KNXDevices knxDevices, ServiceProviders services) {
        this.knxDevices = knxDevices;
        this.services = services;
    }

    public String register(Logic logic) {
        knxDevices.getAllDevices().forEach(device -> device.addListener(logic));
        logicList.add(logic);
        logic.start();
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
