package tools.vlab.kberry.server.logic;

import io.vertx.core.Vertx;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.core.devices.StatusListener;
import tools.vlab.kberry.server.serviceProvider.ServiceProviders;
import tools.vlab.kberry.server.statistics.Statistics;

import java.util.List;
import java.util.UUID;
import java.util.Vector;

@Getter
public abstract class Logic implements StatusListener {

    @Getter(AccessLevel.PRIVATE)
    private final Vector<PositionPath> paths;
    @Setter
    private KNXDevices knxDevices;
    @Setter
    private Statistics statistics;
    @Setter
    private ServiceProviders serviceProviders;
    private Vertx vertx;
    private final String id = UUID.randomUUID().toString();


    protected Logic(Vector<PositionPath> paths) {
        this.paths = paths;
    }

    public boolean contains(PositionPath path) {
        return paths.stream().anyMatch(p -> p.isSame(path));
    }

    public void start(Vertx vertx) {
        this.vertx = vertx;
        this.start();
    }

    protected List<PositionPath> getRequiredPositionPaths() {
        return paths;
    }

    public abstract void stop();

    public abstract void start();


}