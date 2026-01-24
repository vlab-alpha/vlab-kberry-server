package tools.vlab.kberry.server.logic;

import io.vertx.core.Vertx;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.KNXDevice;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.core.devices.StatusListener;
import tools.vlab.kberry.server.serviceProvider.ServiceProviders;
import tools.vlab.kberry.server.statistics.Statistics;

@Getter
public abstract class Logic implements StatusListener {

    @Getter(AccessLevel.PRIVATE)
    private final PositionPath path;
    @Setter
    private KNXDevices knxDevices;
    @Setter
    private Statistics statistics;
    @Setter
    private ServiceProviders serviceProviders;
    private Vertx vertx;
    private final String name;


    protected Logic(String name, PositionPath path) {
        this.name = name;
        this.path = path;
    }

    public boolean isNotSamePosition(KNXDevice device) {
        return !path.isSame(device.getPositionPath());
    }

    public boolean isNotSameRoom(KNXDevice device) {
        return !path.sameRoom(device.getPositionPath());
    }

    public void start(Vertx vertx) {
        this.vertx = vertx;
        this.start();
    }

    protected PositionPath getPositionPath() {
        return path;
    }

    public abstract void stop();

    public abstract void start();

    public String getId() {
        return String.join("@", name, path.getId());
    }

    public boolean hasId(PositionPath positionPath, String name) {
        return positionPath.isSame(positionPath) && this.name.equalsIgnoreCase(name);
    }


}