package tools.vlab.kberry.server;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.vlab.kberry.core.baos.BAOSReader;
import tools.vlab.kberry.core.baos.SerialBAOSConnection;
import tools.vlab.kberry.core.baos.TimeoutException;
import tools.vlab.kberry.core.devices.KNXDevice;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.server.commands.Command;
import tools.vlab.kberry.server.commands.CommandController;
import tools.vlab.kberry.server.logic.Logic;
import tools.vlab.kberry.server.logic.Logics;
import tools.vlab.kberry.server.scheduler.ScheduleEngine;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;
import tools.vlab.kberry.server.serviceProvider.*;
import tools.vlab.kberry.server.statistics.Statistics;
import tools.vlab.kberry.server.statistics.StatisticsScheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class KBerryServer {

    private static final Logger Log = LoggerFactory.getLogger(KBerryServer.class);

    private final WorkerExecutor executor;
    private final SerialBAOSConnection connection;
    @Getter
    private final KNXDevices devices;
    @Getter
    private final CommandController commandController;
    @Getter
    private final Logics logicEngine;

    private KBerryServer(Vertx vertx, SerialBAOSConnection connection, KNXDevices devices, CommandController commandController, Logics logicEngine) {
        this.executor = vertx.createSharedWorkerExecutor("BAOS");
        this.connection = connection;
        this.devices = devices;
        this.commandController = commandController;
        this.logicEngine = logicEngine;
    }

    public void startListening() {
        this.executor.executeBlocking(() -> {
            try {

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
        System.out.println("KBerryServer is now listening... Press Ctrl+C to stop.");
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        logicEngine.stop();
        connection.disconnect();
    }

    public static class Builder {

        private final SerialBAOSConnection connection;
        private final KNXDevices devices;
        private final Set<Command> commands = new HashSet<>();
        private final Set<Logic> logics = new HashSet<>();
        private final String mqttHost;
        private final int mqttPort;
        private final ScheduleEngine scheduler = new ScheduleEngine(new File("schedule"));
        private GoogleCalendarService googleCalendarServiceProvider;
        private IcloudCalendarService icloudCalenderService;

        public Builder(SerialBAOSConnection connection, KNXDevices devices, String mqttAddress, int mqttPort) {
            this.connection = connection;
            this.mqttHost = mqttAddress;
            this.mqttPort = mqttPort;
            this.devices = devices;
        }

        public static Builder create(String serialInterface, String mqttHost, int mqttPort) {
            var connection = new SerialBAOSConnection(serialInterface, 1000, 10);
            var devices = new KNXDevices(connection);
            return new Builder(connection, devices, mqttHost, mqttPort);
        }

        public <T extends KNXDevice> Builder register(T device) {
            this.devices.register(device);
            return this;
        }

        public Builder command(Command command) {
            commands.add(command);
            return this;
        }

        public Builder logic(Logic logic) {
            this.logics.add(logic);
            return this;
        }

        public Builder scheduler(String id, Trigger trigger, Runnable runnable) {
            this.scheduler.start(id, trigger, runnable);
            return this;
        }

        public Builder setGoogleCalendar(Path credPath, String userId, String calendarId, String tokenPath) throws IOException {
            this.icloudCalenderService = null;
            this.googleCalendarServiceProvider = GoogleCalendarService.fromCredentialsFile(credPath, userId, calendarId, tokenPath);
            return this;
        }

        public Builder setICloudCalender(String username, String appPassword, String calendarUrl) {
            this.googleCalendarServiceProvider = null;
            this.icloudCalenderService = IcloudCalendarService.ical(username, appPassword, calendarUrl);
            return this;
        }

        public KBerryServer build() throws IOException, TimeoutException {
            return build("ETSWeinzierlBAOSImport.csv");
        }

        public KBerryServer build(String csvExportFileName) throws IOException, TimeoutException {
            Log.info("KBerryServer export CSV ...");
            devices.exportCSV(Path.of(csvExportFileName));
            Log.info("KBerryServer connect to BAOS ...");
            connection.connect();
            Log.info("KBerryServer Statistics ...");
            Vertx vertx = Vertx.vertx();

            // Statistics
            Statistics statistics = new Statistics();
            var statisticsScheduler = new StatisticsScheduler(statistics, devices);

            Log.info("KBerryServer Service Provider ...");

            // ServiceProvider
            var costWattVerticle = new CostWattVerticle();
            var weatherServiceProvider = new MeteoWeatherVerticle();

            var serviceProvider = new ServiceProviders(
                    costWattVerticle,
                    weatherServiceProvider,
                    googleCalendarServiceProvider != null ? googleCalendarServiceProvider : icloudCalenderService
            );

            Log.info("KBerryServer Logics Init ...");
            // Logic
            var logicEngine = new Logics(vertx, devices, serviceProvider, statistics);
            logics.forEach(logicEngine::register);

            // Commands
            Log.info("KBerryServer MQTT Commands ...");
            var controller = new CommandController(mqttHost, mqttPort, devices, statistics, serviceProvider, scheduler, logicEngine);
            commands.forEach(controller::register);

            Log.info("KBerryServer Deploy Verticles ...");
            return vertx.deployVerticle(statisticsScheduler)
                    .compose(ignore -> vertx.deployVerticle(weatherServiceProvider))
                    .compose(ignore -> vertx.deployVerticle(costWattVerticle))
                    .compose(ignore -> vertx.deployVerticle(controller))
                    .map(ignore -> {
                        Log.info("KBerryServer Build Done ...");
                        return new KBerryServer(vertx, connection, devices, controller, logicEngine);
                    })
                    .await();
        }

    }

}
